package ru.practicum.requestservice.participation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.requestservice.participation.dto.ParticipationRequestDto;
import ru.practicum.requestservice.participation.service.ParticipationRequestService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
public class ParticipationRequestController {

    private final ParticipationRequestService requestService;

    // 2 событие (запрос на участие)
    @PostMapping("/users/{userId}/requests")
    public ResponseEntity<ParticipationRequestDto> createRequest(
            @PathVariable Long userId,
            @RequestParam Long eventId) {

        ParticipationRequestDto createdRequest = requestService.createRequest(userId, eventId);
        return new ResponseEntity<>(createdRequest, HttpStatus.CREATED);
    }

    @GetMapping("/users/{userId}/requests")
    public ResponseEntity<List<ParticipationRequestDto>> getUserRequests(@PathVariable Long userId) {
        List<ParticipationRequestDto> requests = requestService.getUserRequests(userId);
        return ResponseEntity.ok(requests);
    }

    @PatchMapping("/users/{userId}/requests/{requestId}/cancel")
    public ResponseEntity<ParticipationRequestDto> cancelRequest(
            @PathVariable Long userId,
            @PathVariable Long requestId) {
        ParticipationRequestDto canceledRequest = requestService.cancelRequest(userId, requestId);
        return ResponseEntity.ok(canceledRequest);
    }

    @GetMapping("/events/requests/counts")
    public ResponseEntity<List<Object[]>> getConfirmedRequestsCountByEvents(
            @RequestParam List<Long> eventIds) {
        List<Object[]> requestsCountsList = requestService.getConfirmedRequestsCountByEvents(eventIds);
        return ResponseEntity.ok(requestsCountsList);
    }
}