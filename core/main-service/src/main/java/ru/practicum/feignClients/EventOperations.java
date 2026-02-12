package ru.practicum.feignClients;

import org.springframework.cloud.openfeign.FeignClient;
import ru.practicum.participation.dto.event.EventDtoOut;

import java.util.Optional;

@FeignClient(name = "event-service", path = "/")
public interface EventOperations {

    Optional<EventDtoOut> findById(Long eventId);

    boolean existsById(Long eventId);
}
