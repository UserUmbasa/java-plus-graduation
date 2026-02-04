package ru.practicum.event.service;

import ru.practicum.event.dto.EventCreateDto;
import ru.practicum.event.dto.EventDtoOut;
import ru.practicum.event.dto.EventShortDtoOut;
import ru.practicum.event.dto.EventUpdateAdminDto;
import ru.practicum.event.dto.EventUpdateDto;
import ru.practicum.event.model.EventAdminFilter;
import ru.practicum.event.model.EventFilter;

import java.util.Collection;

public interface EventService {

    EventDtoOut add(Long userId, EventCreateDto eventDto);

    EventDtoOut update(Long userId, Long eventId, EventUpdateDto updateRequest);

    EventDtoOut update(Long eventId, EventUpdateAdminDto eventDto);

    EventDtoOut findPublished(Long eventId);

    EventDtoOut find(Long userId, Long eventId);

    Collection<EventShortDtoOut> findShortEventsBy(EventFilter filter);

    Collection<EventDtoOut> findFullEventsBy(EventAdminFilter filter);

    Collection<EventShortDtoOut> findByInitiator(Long userId, Integer offset, Integer limit);
}
