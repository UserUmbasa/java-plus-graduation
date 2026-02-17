package ru.practicum.eventservice.event.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.eventservice.event.dto.EventDtoOut;
import ru.practicum.eventservice.event.dto.EventUpdateAdminDto;
import ru.practicum.eventservice.event.model.EventAdminFilter;
import ru.practicum.eventservice.event.model.EventState;
import ru.practicum.eventservice.event.service.EventService;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static ru.practicum.eventservice.constants.Constants.DATE_TIME_FORMAT;


@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/events")
public class AdminEventController {

    private final EventService eventService;

    @GetMapping("/{eventId}/exists")
    boolean getExistsById(@PathVariable Long eventId) {
        return eventService.getExistsById(eventId);
    }

    @GetMapping("/{eventId}")
    Optional<EventDtoOut> findById(@PathVariable Long eventId) {
        return eventService.findById(eventId);
    }

    @GetMapping
    public Collection<EventDtoOut> getEvents(
            @RequestParam(required = false) List<Long> users,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) List<EventState> states,
            @RequestParam(required = false) @DateTimeFormat(pattern = DATE_TIME_FORMAT) LocalDateTime rangeStart,
            @RequestParam(required = false) @DateTimeFormat(pattern = DATE_TIME_FORMAT) LocalDateTime rangeEnd,
            @RequestParam(defaultValue = "0") Integer offset,
            @RequestParam(defaultValue = "10") Integer limit) {

        log.info("request from Admin: get all events");
        EventAdminFilter filter = EventAdminFilter.builder()
                .users(users)
                .categories(categories)
                .states(states)
                .rangeStart(rangeStart)
                .rangeEnd(rangeEnd)
                .from(offset)
                .size(limit)
                .build();
        return eventService.findFullEventsBy(filter);
    }

    @PatchMapping("/{eventId}")
    public EventDtoOut updateEvent(
            @PathVariable @Min(1) Long eventId,
            @RequestBody @Valid EventUpdateAdminDto eventDto) {
        log.info("request from Admin: update event:{}", eventId);
        return eventService.update(eventId, eventDto);
    }
}
