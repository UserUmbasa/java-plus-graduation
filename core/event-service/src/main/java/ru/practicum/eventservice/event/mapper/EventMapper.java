package ru.practicum.eventservice.event.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.eventservice.category.mapper.CategoryMapper;
import ru.practicum.eventservice.event.dto.EventCreateDto;
import ru.practicum.eventservice.event.dto.EventDtoOut;
import ru.practicum.eventservice.event.dto.EventShortDtoOut;
import ru.practicum.eventservice.event.dto.LocationDto;
import ru.practicum.eventservice.event.model.Event;
import ru.practicum.eventservice.feignClients.UserOperations;


//@UtilityClass
@Component
@RequiredArgsConstructor
public class EventMapper {
    private final UserOperations userOperations;
    public static Event fromDto(EventCreateDto eventDto) {
        return Event.builder()
                .annotation(eventDto.getAnnotation())
                .title(eventDto.getTitle())
                .paid(eventDto.getPaid())
                .eventDate(eventDto.getEventDate())
                .description(eventDto.getDescription())
                .locationLat(eventDto.getLocation().getLat())
                .locationLon(eventDto.getLocation().getLon())
                .participantLimit(eventDto.getParticipantLimit())
                .requestModeration(eventDto.getRequestModeration())
                .build();
    }

    public EventDtoOut toDto(Event event) {
        return EventDtoOut.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .title(event.getTitle())
                .category(CategoryMapper.toDto(event.getCategory()))
                .paid(event.getPaid())
                .eventDate(event.getEventDate())
                .description(event.getDescription())
                .initiator(userOperations.getUser(event.getInitiator()))
                .createdOn(event.getCreatedAt())
                .state(event.getState())
                .confirmedRequests(event.getConfirmedRequests())
                .views(event.getViews())
                .location(new LocationDto(
                        event.getLocationLat(),
                        event.getLocationLon()))
                .participantLimit(event.getParticipantLimit())
                .requestModeration(event.getRequestModeration())
                .build();
    }

    public EventShortDtoOut toShortDto(Event event) {
        return EventShortDtoOut.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .title(event.getTitle())
                .category(CategoryMapper.toDto(event.getCategory()))
                .paid(event.getPaid())
                .eventDate(event.getEventDate())
                .initiator(userOperations.getUser(event.getInitiator()))
                .confirmedRequests(event.getConfirmedRequests())
                .views(event.getViews())
                .build();
    }
}
