package ru.practicum.requestservice.participation.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.requestservice.participation.dto.ParticipationRequestDto;
import ru.practicum.requestservice.participation.model.ParticipationRequest;

@UtilityClass
public class ParticipationRequestMapper {
    public static ParticipationRequestDto toDto(ParticipationRequest r) {
        return ParticipationRequestDto.builder()
                .id(r.getId())
                .created(r.getCreated())
                .event(r.getEvent())
                .requester(r.getRequester())
                .status(r.getStatus().name())
                .build();
    }
}