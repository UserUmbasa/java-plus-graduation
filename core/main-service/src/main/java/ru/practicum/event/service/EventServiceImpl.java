package ru.practicum.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.ViewStatsDTO;
import ru.practicum.category.model.Category;
import ru.practicum.category.repository.CategoryRepository;

import ru.practicum.event.dto.EventCreateDto;
import ru.practicum.event.dto.EventDtoOut;
import ru.practicum.event.dto.EventShortDtoOut;
import ru.practicum.event.dto.EventUpdateAdminDto;
import ru.practicum.event.dto.EventUpdateDto;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventAdminFilter;
import ru.practicum.event.model.EventFilter;
import ru.practicum.event.model.EventState;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.exception.ConditionNotMetException;
import ru.practicum.exception.NoAccessException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.feignClients.UserOperations;
import ru.practicum.participation.repository.ParticipationRequestRepository;
import ru.practicum.statsclient.StatsFeignClient;

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

import static ru.practicum.constants.Constants.STATS_EVENTS_URL;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private static final int MIN_TIME_TO_UNPUBLISHED_EVENT = 2;
    private static final int MIN_TIME_TO_PUBLISHED_EVENT = 1;

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    //private final UserRepository userRepository;
    private final UserOperations userOperations;

    private final CategoryRepository categoryRepository;
    private final ParticipationRequestRepository requestRepository;
    private final StatsFeignClient statsClient; // в настройках переопределение

    @Override
    @Transactional
    public EventDtoOut add(Long userId, EventCreateDto eventDto) {
        validateEventDate(eventDto.getEventDate(), EventState.PENDING);
        Category category = getCategory(eventDto.getCategoryId());
        if (!userOperations.getExistsById(userId)) {
            throw new NotFoundException("User", userId);
        }
        Event event = EventMapper.fromDto(eventDto);
        event.setCategory(category);
        event.setInitiator(userId);
        event = eventRepository.save(event);
        return eventMapper.toDto(event);
    }

    @Override
    @Transactional
    public EventDtoOut update(Long userId, Long eventId, EventUpdateDto eventDto) {
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
        return eventMapper.toDto(updated);
    }

    @Override
    @Transactional
    public EventDtoOut update(Long eventId, EventUpdateAdminDto eventDto) {
        Event event = getEvent(eventId);
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
        return eventMapper.toDto(saved);
    }

    @Override
    public EventDtoOut findPublished(Long eventId) {
        Event event = eventRepository.findPublishedById(eventId)
                .orElseThrow(() -> new NotFoundException("Event", eventId));
        enrichWithStats(Collections.singletonList(event));
        return eventMapper.toDto(event);
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

    private Long getViewsCount(Long eventId) {
        List<ViewStatsDTO> result = statsClient.getStats(
                LocalDateTime.now().minusYears(10),
                LocalDateTime.now().plusYears(10),
                List.of(STATS_EVENTS_URL + eventId),
                true);
        return result.stream().count();
    }

    @Override
    public EventDtoOut find(Long userId, Long eventId) {
//        if (!userRepository.existsById(userId)) {
//            throw new NotFoundException("User", userId);
//        }
        if (!userOperations.getExistsById(userId)) {
            throw new NotFoundException("User", userId);
        }
        Event event = getEvent(eventId);
        if (!event.getInitiator().equals(userId)) {
            throw new NoAccessException("Только инициатор может просматривать это событие");
        }
        enrichWithStats(Collections.singletonList(event));
        return eventMapper.toDto(event);
    }

    @Override
    public Collection<EventShortDtoOut> findShortEventsBy(EventFilter filter) {
        Specification<Event> spec = buildSpecification(filter);
        return findBy(spec, filter.getPageable()).stream()
                .map(eventMapper::toShortDto)
                .toList();
    }

    @Override
    public Collection<EventDtoOut> findFullEventsBy(EventAdminFilter filter) {
        Specification<Event> spec = buildSpecification(filter);
        return findBy(spec, filter.getPageable()).stream()
                .map(eventMapper::toDto)
                .toList();
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
//        if (!userRepository.existsById(userId)) {
//            throw new NotFoundException("User", userId);
//        }
        if (!userOperations.getExistsById(userId)) {
            throw new NotFoundException("User", userId);
        }

        Pageable pageable = PageRequest.of(offset / limit, limit, Sort.by("id"));
        Page<Event> eventPage = eventRepository.findByInitiatorId(userId, pageable);
        List<Event> events = eventPage.getContent();
        enrichWithStatsCollection(events);

        return events.stream()
                .map(eventMapper::toShortDto)
                .toList();
    }

    @Transactional(readOnly = true)
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

        List<Object[]> requestsCountsList = requestRepository.findConfirmedRequestCountsByEventIds(eventIds);

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

//    @SuppressWarnings("UnusedReturnValue")
//    private User getUser(Long userId) {
//        return userRepository.findById(userId)
//                .orElseThrow(() -> new NotFoundException("User", userId));
//    }

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
}
