package ru.practicum.feignClients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.comment.dto.event.EventDtoOut;

import java.util.Optional;

@FeignClient(name = "event-service", path = "/")
public interface EventOperations {

    @GetMapping("/admin/events/{eventId}")
    Optional<EventDtoOut> findById(@PathVariable Long eventId);

    @GetMapping("/admin/events/{eventId}/exists")
    boolean getExistsById(@PathVariable Long eventId);
}
