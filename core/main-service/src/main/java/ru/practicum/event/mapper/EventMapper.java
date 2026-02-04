package ru.practicum.event.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.category.mapper.CategoryMapper;
import ru.practicum.event.dto.EventCreateDto;
import ru.practicum.event.dto.EventDtoOut;
import ru.practicum.event.dto.EventShortDtoOut;
import ru.practicum.event.dto.LocationDto;
import ru.practicum.event.model.Event;
import ru.practicum.user.mapper.UserMapper;

@UtilityClass
public class EventMapper {
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

    public static EventDtoOut toDto(Event event) {
        return EventDtoOut.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .title(event.getTitle())
                .category(CategoryMapper.toDto(event.getCategory()))
                .paid(event.getPaid())
                .eventDate(event.getEventDate())
                .description(event.getDescription())
                .initiator(UserMapper.toDto(event.getInitiator()))
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

    public static EventShortDtoOut toShortDto(Event event) {
        return EventShortDtoOut.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .title(event.getTitle())
                .category(CategoryMapper.toDto(event.getCategory()))
                .paid(event.getPaid())
                .eventDate(event.getEventDate())
                .initiator(UserMapper.toDto(event.getInitiator()))
                .confirmedRequests(event.getConfirmedRequests())
                .views(event.getViews())
                .build();
    }
}
