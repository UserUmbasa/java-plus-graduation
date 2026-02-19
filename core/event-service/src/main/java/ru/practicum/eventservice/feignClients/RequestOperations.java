package ru.practicum.eventservice.feignClients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.eventservice.event.dto.participation.ParticipationRequestDto;

import java.util.List;


@FeignClient(name = "request-service", path = "/events/requests/counts")
public interface RequestOperations {

    @GetMapping
    List<Object[]> getConfirmedRequestsCountByEvents(
            @RequestParam List<Long> eventIds);

    @GetMapping("/{userId}/{eventId}")
    ParticipationRequestDto findByRequesterIdAndEventId(
            @PathVariable Long userId,
            @PathVariable Long eventId);
}
