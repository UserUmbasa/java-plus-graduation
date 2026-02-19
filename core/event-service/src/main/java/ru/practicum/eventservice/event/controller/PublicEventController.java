package ru.practicum.eventservice.event.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;

import org.springframework.web.bind.annotation.*;
//import ru.practicum.client.UserActionClient;
import ru.practicum.eventservice.feignClients.UserActionClient;
import ru.practicum.eventservice.event.dto.event.EventDtoOut;
import ru.practicum.eventservice.event.dto.event.EventShortDtoOut;
import ru.practicum.eventservice.event.model.EventFilter;
import ru.practicum.eventservice.event.model.EventState;
import ru.practicum.eventservice.event.service.EventService;
import ru.practicum.eventservice.exception.InvalidRequestException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;

import static ru.practicum.eventservice.constants.Constants.DATE_TIME_FORMAT;


@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/events")
public class PublicEventController {

    private final EventService eventService;
    private final UserActionClient userActionClient;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @GetMapping
    public Collection<EventShortDtoOut> getEvents(
            @Size(min = 3, max = 1000, message = "Текст должен быть длиной от 3 до 1000 символов")
            @RequestParam(required = false) String text,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) Boolean paid,
            @RequestParam(required = false) @DateTimeFormat(pattern = DATE_TIME_FORMAT) LocalDateTime rangeStart,
            @RequestParam(required = false) @DateTimeFormat(pattern = DATE_TIME_FORMAT) LocalDateTime rangeEnd,
            @RequestParam(defaultValue = "false") Boolean onlyAvailable,
            @RequestParam(defaultValue = "EVENT_DATE") String sort,
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size,
            HttpServletRequest request) {

        EventFilter filter = EventFilter.builder()
                .text(text)
                .categories(categories)
                .paid(paid)
                .rangeStart(rangeStart)
                .rangeEnd(rangeEnd)
                .onlyAvailable(onlyAvailable)
                .sort(sort)
                .from(from)
                .size(size)
                .state(EventState.PUBLISHED)
                .build();

        if (filter.getRangeStart() != null && filter.getRangeEnd() != null) {
            if (filter.getRangeStart().isAfter(filter.getRangeEnd())) {
                throw new InvalidRequestException("Дата начала должна быть раньше даты конца");
            }
        }

        return eventService.findShortEventsBy(filter);
    }

    // событие - Просмотр мероприятия
    @GetMapping("/{eventId}")
    public EventDtoOut get(@PathVariable @Min(1) Long eventId,
                           HttpServletRequest request) {
        log.debug("запрос на публикацию идентификатора события:{}", eventId);
        EventDtoOut dtoOut = eventService.findPublished(eventId);
        userActionClient.sendView(dtoOut.getInitiator().getId(), dtoOut.getId());
        return dtoOut;
    }

    @PutMapping("/{eventId}/like")
    public void likeEvent(@PathVariable long eventId, @RequestHeader("X-EWM-USER-ID") long userId) {
        log.debug("запрос на публикацию лайка к события:{}", eventId);
        eventService.likeEvent(userId, eventId);
        userActionClient.sendLike(userId, eventId);
    }

}

