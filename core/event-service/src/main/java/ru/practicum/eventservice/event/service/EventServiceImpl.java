package ru.practicum.eventservice.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import ru.practicum.dto.ViewStatsDTO;
import ru.practicum.eventservice.category.model.Category;
import ru.practicum.eventservice.category.repository.CategoryRepository;

import ru.practicum.eventservice.event.dto.EventCreateDto;
import ru.practicum.eventservice.event.dto.EventDtoOut;
import ru.practicum.eventservice.event.dto.EventShortDtoOut;
import ru.practicum.eventservice.event.dto.EventUpdateAdminDto;
import ru.practicum.eventservice.event.dto.EventUpdateDto;
import ru.practicum.eventservice.event.dto.user.UserDtoOut;
import ru.practicum.eventservice.event.mapper.EventMapper;
import ru.practicum.eventservice.event.model.Event;
import ru.practicum.eventservice.event.model.EventAdminFilter;
import ru.practicum.eventservice.event.model.EventFilter;
import ru.practicum.eventservice.event.model.EventState;
import ru.practicum.eventservice.event.repository.EventRepository;
import ru.practicum.eventservice.exception.ConditionNotMetException;
import ru.practicum.eventservice.exception.NoAccessException;
import ru.practicum.eventservice.exception.NotFoundException;
import ru.practicum.eventservice.feignClients.RequestOperations;
import ru.practicum.eventservice.feignClients.UserOperations;
import ru.practicum.eventservice.statsclient.StatsFeignClient;

import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ru.practicum.eventservice.constants.Constants.STATS_EVENTS_URL;


@Slf4j
@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private static final int MIN_TIME_TO_UNPUBLISHED_EVENT = 2;
    private static final int MIN_TIME_TO_PUBLISHED_EVENT = 1;

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final UserOperations userOperations;
    private final CategoryRepository categoryRepository;
    private final RequestOperations requestOperations;
    private final StatsFeignClient statsClient;
    private final TransactionTemplate transactionTemplate;

    @Override
    public EventDtoOut add(Long userId, EventCreateDto eventDto) {
        // 1. Делаем всё "внешнее" и валидацию
        validateEventDate(eventDto.getEventDate(), EventState.PENDING);
        UserDtoOut user = userOperations.getUser(userId);
        if (user == null) {
            throw new NotFoundException("User", userId);
        }
        // 2. Открываем транзакцию только для работы с БД
        return transactionTemplate.execute(status -> {
            Category category = getCategory(eventDto.getCategoryId());
            Event event = EventMapper.fromDto(eventDto);
            event.setCategory(category);
            event.setInitiator(userId);
            event = eventRepository.save(event);
            return eventMapper.toDto(event, user);
        });
    }

    @Override
    public EventDtoOut update(Long userId, Long eventId, EventUpdateDto eventDto) {
        UserDtoOut user = userOperations.getUser(userId);
        if (user == null) {
            throw new NotFoundException("User", userId);
        }
        return transactionTemplate.execute(status -> {
            Event event = getEvent(eventId);
            if (!event.getInitiator().equals(userId)) {
                throw new NoAccessException("Редактировать событие может только инициатор");
            }
            if (event.getState() == EventState.PUBLISHED) {
                throw new ConditionNotMetException("Не удается обновить опубликованное событие");
            }
            Optional.ofNullable(eventDto.getTitle()).ifPresent(event::setTitle);
            Optional.ofNullable(eventDto.getAnnotation()).ifPresent(event::setAnnotation);
            Optional.ofNullable(eventDto.getDescription()).ifPresent(event::setDescription);
            Optional.ofNullable(eventDto.getPaid()).ifPresent(event::setPaid);
            Optional.ofNullable(eventDto.getLocation()).ifPresent(loc -> {
                event.setLocationLat(loc.getLat());
                event.setLocationLon(loc.getLon());
            });
            Optional.ofNullable(eventDto.getParticipantLimit()).ifPresent(event::setParticipantLimit);
            Optional.ofNullable(eventDto.getRequestModeration()).ifPresent(event::setRequestModeration);
            if (eventDto.getCategoryId() != null
                    && !eventDto.getCategoryId().equals(event.getCategory().getId())) {
                Category category = categoryRepository.findById(eventDto.getCategoryId())
                        .orElseThrow(() -> new NotFoundException("Category", eventDto.getCategoryId()));
                event.setCategory(category);
            }
            if (eventDto.getEventDate() != null) {
                validateEventDate(eventDto.getEventDate(), event.getState());
                event.setEventDate(eventDto.getEventDate());
            }
            if (eventDto.getStateAction() != null) {
                switch (eventDto.getStateAction()) {
                    case SEND_TO_REVIEW -> event.setState(EventState.PENDING);
                    case CANCEL_REVIEW -> event.setState(EventState.CANCELED);
                }
            }
            Event updated = eventRepository.save(event);
            return eventMapper.toDto(updated, user);
        });
    }

    @Override
    public EventDtoOut update(Long eventId, EventUpdateAdminDto eventDto) {
        Event event = getEvent(eventId);
        UserDtoOut user = userOperations.getUser(event.getInitiator());
        return transactionTemplate.execute(status -> {
            Optional.ofNullable(eventDto.getTitle()).ifPresent(event::setTitle);
            Optional.ofNullable(eventDto.getAnnotation()).ifPresent(event::setAnnotation);
            Optional.ofNullable(eventDto.getDescription()).ifPresent(event::setDescription);
            Optional.ofNullable(eventDto.getParticipantLimit()).ifPresent(event::setParticipantLimit);
            Optional.ofNullable(eventDto.getPaid()).ifPresent(event::setPaid);
            Optional.ofNullable(eventDto.getLocation()).ifPresent(loc -> {
                event.setLocationLat(loc.getLat());
                event.setLocationLon(loc.getLon());
            });
            Optional.ofNullable(eventDto.getParticipantLimit()).ifPresent(event::setParticipantLimit);
            Optional.ofNullable(eventDto.getRequestModeration()).ifPresent(event::setRequestModeration);
            if (eventDto.getEventDate() != null) {
                validateEventDate(eventDto.getEventDate(), event.getState());
                event.setEventDate(eventDto.getEventDate());
            }
            if (eventDto.getStateAction() != null) {
                switch (eventDto.getStateAction()) {
                    case PUBLISH_EVENT -> publishEvent(event);
                    case REJECT_EVENT -> rejectEvent(event);
                }
            }
            Event saved = eventRepository.save(event);
            return eventMapper.toDto(saved, user);
        });
    }

    @Override
    public EventDtoOut findPublished(Long eventId) {
        Event event = eventRepository.findPublishedById(eventId)
                .orElseThrow(() -> new NotFoundException("Event", eventId));
        UserDtoOut user = userOperations.getUser(event.getInitiator());
        return transactionTemplate.execute(status -> {
            enrichWithStats(Collections.singletonList(event));
            return eventMapper.toDto(event, user);
        });
    }

    private void enrichWithStats(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        enrichEventsWithConfirmedRequests(events);
        enrichWithViewsCountCollection(events);
    }

    void enrichWithStatsCollection(Collection<Event> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        List<Event> eventList = new ArrayList<>(events);
        enrichEventsWithConfirmedRequests(eventList);
        enrichWithViewsCountCollection(eventList);
    }

    private void enrichWithViewsCountCollection(Collection<Event> events) {
        if (events.isEmpty()) {
            return;
        }

        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .collect(Collectors.toList());

        Map<Long, Long> eventViewsMap = getViewsCountForEvents(eventIds);

        events.forEach(event ->
                event.setViews(eventViewsMap.getOrDefault(event.getId(), 0L))
        );
    }

    private Map<Long, Long> getViewsCountForEvents(List<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<String> uris = eventIds.stream()
                .map(id -> STATS_EVENTS_URL + id)
                .collect(Collectors.toList());

        List<ViewStatsDTO> stats = statsClient.getStats(
                LocalDateTime.now().minusYears(10),
                LocalDateTime.now().plusYears(10),
                uris,
                true);

        return stats.stream()
                .collect(Collectors.toMap(
                        stat -> extractEventIdFromUri(stat.getUri()),
                        ViewStatsDTO::getHits
                ));
    }

    private Long extractEventIdFromUri(String uri) {
        try {
            return Long.parseLong(uri.substring(STATS_EVENTS_URL.length()));
        } catch (NumberFormatException e) {
            log.warn("Failed to extract eventId from uri: {}", uri);
            return -1L;
        }
    }

    @Override
    public EventDtoOut find(Long userId, Long eventId) {
        UserDtoOut user = userOperations.getUser(userId);
        if (user == null) {
            throw new NotFoundException("User", userId);
        }
        return transactionTemplate.execute(status -> {
            Event event = getEvent(eventId);
            if (!event.getInitiator().equals(userId)) {
                throw new NoAccessException("Только инициатор может просматривать это событие");
            }
            enrichWithStats(Collections.singletonList(event));
            return eventMapper.toDto(event, user);
        });
    }

    @Override
    public Collection<EventShortDtoOut> findShortEventsBy(EventFilter filter) {
        Specification<Event> spec = buildSpecification(filter);
        Collection<Event> events = transactionTemplate.execute(status -> findBy(spec, filter.getPageable()));
        Map<Long, UserDtoOut> usersMap = getUsersMap(events);
        return events.stream()
                .map(event -> eventMapper.toShortDto(event, usersMap.get(event.getInitiator())))
                .collect(Collectors.toList());
    }

    @Override
    public Collection<EventDtoOut> findFullEventsBy(EventAdminFilter filter) {
        Specification<Event> spec = buildSpecification(filter);
        Collection<Event> events = transactionTemplate.execute(status -> findBy(spec, filter.getPageable()));
        Map<Long, UserDtoOut> usersMap = getUsersMap(events);
        return events.stream()
                .map(event -> eventMapper.toDto(event, usersMap.get(event.getInitiator())))
                .collect(Collectors.toList());
    }

    private Collection<Event> findBy(Specification<Event> spec, Pageable pageable) {
        Collection<Event> events = eventRepository.findAll(spec, pageable).getContent();
        enrichWithStatsCollection(events);
        return events;
    }

    private Specification<Event> buildSpecification(EventAdminFilter filter) {
        return Stream.of(
                        optionalSpec(EventSpecifications.withUsers(filter.getUsers())),
                        optionalSpec(EventSpecifications.withCategoriesIn(filter.getCategories())),
                        optionalSpec(EventSpecifications.withStatesIn(filter.getStates())),
                        optionalSpec(EventSpecifications.withRangeStart(filter.getRangeStart())),
                        optionalSpec(EventSpecifications.withRangeEnd(filter.getRangeEnd()))
                )
                .filter(Objects::nonNull)
                .reduce(Specification::and)
                .orElse((root, query, cb) -> cb.conjunction());
    }

    private Specification<Event> buildSpecification(EventFilter filter) {
        return Stream.of(
                        optionalSpec(EventSpecifications.withTextContains(filter.getText())),
                        optionalSpec(EventSpecifications.withCategoriesIn(filter.getCategories())),
                        optionalSpec(EventSpecifications.withPaid(filter.getPaid())),
                        optionalSpec(EventSpecifications.withState(filter.getState())),
                        optionalSpec(EventSpecifications.withOnlyAvailable(filter.getOnlyAvailable())),
                        optionalSpec(EventSpecifications.withRangeStart(filter.getRangeStart())),
                        optionalSpec(EventSpecifications.withRangeEnd(filter.getRangeEnd()))
                )
                .filter(Objects::nonNull)
                .reduce(Specification::and)
                .orElse((root, query, cb) -> cb.conjunction());
    }

    private static <T> Specification<T> optionalSpec(Specification<T> spec) {
        return spec;
    }

    @Override
    public Collection<EventShortDtoOut> findByInitiator(Long userId, Integer offset, Integer limit) {
        // 1. Вместо проверки существования, сразу получаем данные пользователя.
        // Если getUser вернет null или выкинет 404 — мы сэкономим один вызов.
        UserDtoOut initiator = userOperations.getUser(userId);
        if (initiator == null) {
            throw new NotFoundException("User", userId);
        }

        // 2. Получаем события из БД
        Pageable pageable = PageRequest.of(offset / limit, limit, Sort.by("id"));
        List<Event> events = eventRepository.findByInitiatorId(userId, pageable).getContent();

        // Если событий нет, можно сразу вернуть пустой список
        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. Обогащаем статистикой (сетевой вызов к Stats Service)
        enrichWithStatsCollection(events);

        // 4. Маппим. Нам НЕ НУЖНА usersMap, так как инициатор у всех один — наш 'initiator'
        return events.stream()
                .map(event -> eventMapper.toShortDto(event, initiator))
                .collect(Collectors.toList());
    }



    @Override
    public boolean getExistsById(Long eventId) {
        return eventRepository.existsById(eventId);
    }

    @Override
    public Optional<EventDtoOut> findById(Long eventId) {
        Event event = getEvent(eventId);
        UserDtoOut user = userOperations.getUser(event.getInitiator()); // !!!!!!!!!!
        return Optional.ofNullable(eventMapper.toDto(event, user));
    }

    void enrichEventsWithConfirmedRequests(Collection<Event> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        Map<Long, Integer> confirmedRequestsCounts = getConfirmedRequestsCountsByEventIds(events);
        applyConfirmedRequestsCountsToEvents(events, confirmedRequestsCounts);
    }

    private Map<Long, Integer> getConfirmedRequestsCountsByEventIds(Collection<Event> events) {
        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .collect(Collectors.toList());

        if (eventIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Object[]> requestsCountsList = requestOperations.getConfirmedRequestsCountByEvents(eventIds);

        return requestsCountsList.stream()
                .collect(Collectors.toMap(
                        arr -> ((Number) arr[0]).longValue(),
                        arr -> ((Number) arr[1]).intValue()
                ));
    }

    private void applyConfirmedRequestsCountsToEvents(Collection<Event> events, Map<Long, Integer> countsMap) {
        events.forEach(event -> {
            Integer count = countsMap.getOrDefault(event.getId(), 0);
            event.setConfirmedRequests(count);
        });
    }

    private void validateEventDate(LocalDateTime eventDate, EventState state) {
        if (eventDate == null) {
            throw new IllegalArgumentException("Значение EventDate равно нулю");
        }
        int hours = state == EventState.PUBLISHED
                ? MIN_TIME_TO_PUBLISHED_EVENT
                : MIN_TIME_TO_UNPUBLISHED_EVENT;
        if (eventDate.isBefore(LocalDateTime.now().plusHours(hours))) {
            String message = "Дата события должна быть не ранее, чем через несколько часов после даты события"
                    .formatted(hours, state == EventState.PUBLISHED ? "publishing" : "current");
            throw new ConditionNotMetException(message);
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    private Category getCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category", categoryId));
    }

    @SuppressWarnings("UnusedReturnValue")
    private Event getEvent(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event", eventId));
    }

    private void publishEvent(Event event) {
        if (event.getState() != EventState.PENDING) {
            throw new ConditionNotMetException("Для публикации события должны иметь статус ожидающие");
        }
        validateEventDate(event.getEventDate(), EventState.PUBLISHED);
        event.setState(EventState.PUBLISHED);
        event.setPublishedOn(LocalDateTime.now());
    }

    private void rejectEvent(Event event) {
        if (event.getState() == EventState.PUBLISHED) {
            throw new ConditionNotMetException("Опубликованные события не могут быть отклонены");
        }
        event.setState(EventState.CANCELED);
    }

    private Map<Long, UserDtoOut> getUsersMap(Collection<Event> events) {
        if (events.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> userIds = events.stream()
                .map(Event::getInitiator)
                .distinct()
                .collect(Collectors.toList());
        return userOperations.getUsers(userIds)
                .stream()
                .collect(Collectors.toMap(UserDtoOut::getId, user -> user));
    }
}
