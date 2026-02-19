package ru.practicum.requestservice.participation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import ru.practicum.requestservice.exception.ConditionNotMetException;
import ru.practicum.requestservice.exception.ForbiddenException;
import ru.practicum.requestservice.exception.NoAccessException;
import ru.practicum.requestservice.exception.NotFoundException;
import ru.practicum.requestservice.feignClients.EventOperations;
import ru.practicum.requestservice.feignClients.UserOperations;
import ru.practicum.requestservice.participation.dto.event.EventRequestStatusUpdateRequest;
import ru.practicum.requestservice.participation.dto.event.EventRequestStatusUpdateResult;
import ru.practicum.requestservice.participation.dto.ParticipationRequestDto;
import ru.practicum.requestservice.participation.dto.event.EventDtoOut;
import ru.practicum.requestservice.participation.dto.event.EventState;
import ru.practicum.requestservice.participation.mapper.ParticipationRequestMapper;
import ru.practicum.requestservice.participation.model.ParticipationRequest;
import ru.practicum.requestservice.participation.model.RequestStatus;
import ru.practicum.requestservice.participation.repository.ParticipationRequestRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static ru.practicum.requestservice.participation.model.RequestStatus.CANCELED;
import static ru.practicum.requestservice.participation.model.RequestStatus.CONFIRMED;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParticipationRequestServiceImpl implements ParticipationRequestService {

    private final UserOperations userOperations;
    private final EventOperations eventOperations;
    private final ParticipationRequestRepository requestRepo;
    private final TransactionTemplate transactionTemplate;

    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        log.info("Пользователь {} пытается создать запрос участия для события {}", userId, eventId);
        EventDtoOut event = getEventById(eventId); // сеть EventOperations
        checkUserNotExists(userId); // сеть UserOperations
        checkRequestNotExists(userId, eventId); //база
        checkNotEventInitiator(userId, event); // локально
        checkEventIsPublished(event); // локально
        checkParticipantLimit(event, eventId); //локально

        RequestStatus stat = determineRequestStatus(event); // приват

        ParticipationRequest request = new ParticipationRequest();
        request.setRequester(userId);
        request.setEvent(eventId);
        request.setCreated(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));
        request.setStatus(stat);
        log.info("Создана заявка от пользователя {} на событие {} со статусом {}", userId, eventId, stat);

        return transactionTemplate.execute(status -> {
            ParticipationRequest savedRequest = requestRepo.save(request);
            return ParticipationRequestMapper.toDto(savedRequest);
        });
    }

    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        if (!userOperations.getExistsById(userId)) {
            throw new NotFoundException("User", userId);
        }
        return transactionTemplate.execute(status -> {
            return requestRepo.findAllByRequester(userId).stream()
                    .map(ParticipationRequestMapper::toDto)
                    .toList();
        });
    }

    @Override
    public EventRequestStatusUpdateResult updateRequestStatuses(Long userId, Long eventId,
                                                                EventRequestStatusUpdateRequest request) {
        EventDtoOut event = getEventWithCheck(userId, eventId);

        return transactionTemplate.execute(status -> {
            List<ParticipationRequest> requests = getPendingRequestsOrThrow(request.getRequestIds());
            return switch (request.getStatus()) {
                case "CONFIRMED" -> confirmRequests(event, requests);
                case "REJECTED" -> rejectRequests(requests);
                default -> throw new IllegalArgumentException("Неправильный статус: " + request.getStatus());
            };
        });
    }

    @Transactional
    @Override
    public List<Object[]> getConfirmedRequestsCountByEvents(List<Long> eventIds) {
        return requestRepo.findConfirmedRequestCountsByEventIds(eventIds);
    }

    @Override
    public ParticipationRequestDto findByRequesterAndEvent(Long userId, Long eventId) {
        ParticipationRequest request = requestRepo.findByRequesterAndEvent(userId, eventId)
                .orElseThrow(() -> new NotFoundException("Запроса пользователя на мероприятие не найдено", userId));
        return ParticipationRequestMapper.toDto(request);
    }

    @Override
    public List<ParticipationRequestDto> getRequestsForEvent(Long eventId, Long initiatorId) {
        log.debug("getRequestsForEvent: {} of user: {}", eventId, initiatorId);
        checkUserNotExists(initiatorId);
        EventDtoOut event = getEventById(eventId);
        if (!event.getInitiator().getId().equals(initiatorId)) {
            throw new NoAccessException("Только инициатор может просматривать запросы на проведение мероприятия");
        }
        return transactionTemplate.execute(status -> {
            List<ParticipationRequest> allByEventId = requestRepo.findAllByEvent(eventId);
            return allByEventId.stream()
                    .map(ParticipationRequestMapper::toDto)
                    .toList();
        });
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        log.info("Пользователь {} отменяет заявку с ID {}", userId, requestId);
        ParticipationRequest request = requestRepo.findById(requestId)
                .orElseThrow(() -> new NotFoundException("ParticipationRequest", requestId));
        if (!request.getRequester().equals(userId)) {
            throw new ForbiddenException("Отменить его может только автор заявки.");
        }
        request.setStatus(CANCELED);
        return ParticipationRequestMapper.toDto(requestRepo.save(request));
    }

    private EventDtoOut getEventById(Long eventId) {
        return eventOperations.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event", eventId));
    }

    private void checkUserNotExists(Long userId) {
        if (!userOperations.getExistsById(userId)) {
            throw new NotFoundException("User", userId);
        }
    }

    private void checkRequestNotExists(Long userId, Long eventId) {
        if (requestRepo.existsByRequesterAndEvent(userId, eventId)) {
            throw new ConditionNotMetException("Заявка на участие уже отправлена.");
        }
    }

    private void checkNotEventInitiator(Long userId, EventDtoOut event) {
        if (event.getInitiator().getId().equals(userId)) {
            throw new ConditionNotMetException("Заявка на участие уже отправлена.");
        }
    }

    private void checkEventIsPublished(EventDtoOut event) {
        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new ConditionNotMetException("Невозможно принять участие в неопубликованном мероприятии.");
        }
    }

    private void checkParticipantLimit(EventDtoOut event, Long eventId) {
        long confirmed = requestRepo.countByEventAndStatus(eventId, CONFIRMED);
        if (event.getParticipantLimit() > 0 && confirmed >= event.getParticipantLimit()) {
            throw new ConditionNotMetException("Лимит участников мероприятия достигнут.");
        }
    }

    private RequestStatus determineRequestStatus(EventDtoOut event) {
        return (!Boolean.TRUE.equals(event.getRequestModeration()) || event.getParticipantLimit() == 0)
                ? CONFIRMED
                : RequestStatus.PENDING;
    }

    private EventDtoOut getEventWithCheck(Long userId, Long eventId) {
        EventDtoOut event = eventOperations.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event", eventId));
        if (!event.getInitiator().getId().equals(userId)) {
            throw new ForbiddenException("Пользователь не является инициатором события");
        }
        if (!EventState.PUBLISHED.equals(event.getState())) {
            throw new ConditionNotMetException("Мероприятие должно быть опубликовано");
        }
        return event;
    }

    private List<ParticipationRequest> getPendingRequestsOrThrow(List<Long> requestIds) {
        List<ParticipationRequest> requests = requestRepo.findAllById(requestIds);
        boolean hasNonPending = requests.stream()
                .anyMatch(r -> r.getStatus() != RequestStatus.PENDING);
        if (hasNonPending) {
            throw new ConditionNotMetException("Запрос должен иметь статус ОЖИДАЮЩИЙ");
        }
        return requests;
    }

    private EventRequestStatusUpdateResult confirmRequests(EventDtoOut event, List<ParticipationRequest> requests) {
        checkIfLimitAvailableOrThrow(event);
        int limit = event.getParticipantLimit();
        long confirmedCount = requestRepo.countByEventAndStatus(event.getId(), CONFIRMED);
        int available = limit - (int) confirmedCount;
        List<ParticipationRequest> confirmed = new ArrayList<>();
        List<ParticipationRequest> rejected = new ArrayList<>();
        for (ParticipationRequest request : requests) {
            if (shouldAutoConfirm(event)) {
                confirmRequest(request, confirmed);
            } else if (available > 0) {
                confirmRequest(request, confirmed);
                available--;
            } else {
                rejectRequest(request, rejected);
            }
        }
        requestRepo.saveAll(requests);
        return new EventRequestStatusUpdateResult(
                confirmed.stream().map(ParticipationRequestMapper::toDto).toList(),
                rejected.stream().map(ParticipationRequestMapper::toDto).toList()
        );
    }

    private void checkIfLimitAvailableOrThrow(EventDtoOut event) {
        int limit = event.getParticipantLimit();
        long confirmedCount = requestRepo.countByEventAndStatus(event.getId(), CONFIRMED);
        if (limit != 0 && Boolean.TRUE.equals(event.getRequestModeration()) && confirmedCount >= limit) {
            throw new ConditionNotMetException("Лимит участников мероприятия достигнет");
        }
    }

    private boolean shouldAutoConfirm(EventDtoOut event) {
        return event.getParticipantLimit() == 0 || Boolean.FALSE.equals(event.getRequestModeration());
    }

    private void confirmRequest(ParticipationRequest request, List<ParticipationRequest> confirmed) {
        request.setStatus(CONFIRMED);
        confirmed.add(request);
    }

    private void rejectRequest(ParticipationRequest request, List<ParticipationRequest> rejected) {
        request.setStatus(RequestStatus.REJECTED);
        rejected.add(request);
    }

    private EventRequestStatusUpdateResult rejectRequests(List<ParticipationRequest> requests) {
        for (ParticipationRequest r : requests) {
            r.setStatus(RequestStatus.REJECTED);
        }
        requestRepo.saveAll(requests);
        return new EventRequestStatusUpdateResult(
                List.of(),
                requests.stream().map(ParticipationRequestMapper::toDto).toList()
        );
    }
}