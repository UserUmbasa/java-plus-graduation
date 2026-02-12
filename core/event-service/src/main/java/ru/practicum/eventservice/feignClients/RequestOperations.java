package ru.practicum.eventservice.feignClients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "main-service", path = "/events/requests/counts")
public interface RequestOperations {
    @GetMapping
    public List<Object[]> getConfirmedRequestsCountByEvents(
            @RequestParam List<Long> eventIds);
}
