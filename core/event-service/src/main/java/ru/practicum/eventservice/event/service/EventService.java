package ru.practicum.eventservice.event.service;

import ru.practicum.eventservice.event.dto.event.EventCreateDto;
import ru.practicum.eventservice.event.dto.event.EventDtoOut;
import ru.practicum.eventservice.event.dto.event.EventShortDtoOut;
import ru.practicum.eventservice.event.dto.event.EventUpdateAdminDto;
import ru.practicum.eventservice.event.dto.event.EventUpdateDto;
import ru.practicum.eventservice.event.model.EventAdminFilter;
import ru.practicum.eventservice.event.model.EventFilter;

import java.util.Collection;
import java.util.Optional;

public interface EventService {

    EventDtoOut add(Long userId, EventCreateDto eventDto);

    EventDtoOut update(Long userId, Long eventId, EventUpdateDto updateRequest);

    EventDtoOut update(Long eventId, EventUpdateAdminDto eventDto);

    EventDtoOut findPublished(Long eventId);

    EventDtoOut find(Long userId, Long eventId);

    Collection<EventShortDtoOut> findShortEventsBy(EventFilter filter);

    Collection<EventDtoOut> findFullEventsBy(EventAdminFilter filter);

    Collection<EventShortDtoOut> findByInitiator(Long userId, Integer offset, Integer limit);

    boolean getExistsById(Long eventId);

    Optional<EventDtoOut> findById(Long eventId);

    void likeEvent(long userId, long eventId);
}
