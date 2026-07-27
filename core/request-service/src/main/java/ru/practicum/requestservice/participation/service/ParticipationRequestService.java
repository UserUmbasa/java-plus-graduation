package ru.practicum.requestservice.participation.service;

import ru.practicum.requestservice.participation.dto.event.EventRequestStatusUpdateRequest;
import ru.practicum.requestservice.participation.dto.event.EventRequestStatusUpdateResult;
import ru.practicum.requestservice.participation.dto.ParticipationRequestDto;

import java.util.List;

public interface ParticipationRequestService {

    ParticipationRequestDto createRequest(Long userId, Long eventId);

    List<ParticipationRequestDto> getUserRequests(Long userId);

    List<ParticipationRequestDto> getRequestsForEvent(Long eventId, Long initiatorId);

    ParticipationRequestDto cancelRequest(Long userId, Long requestId);

    EventRequestStatusUpdateResult updateRequestStatuses(Long userId, Long eventId, EventRequestStatusUpdateRequest request);

    List<Object[]> getConfirmedRequestsCountByEvents(List<Long> eventIds);

    ParticipationRequestDto findByRequesterAndEvent(Long requestId, Long eventId);
}