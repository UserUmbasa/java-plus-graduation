package ru.practicum.statsclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.dto.EndpointHitDTO;
import ru.practicum.dto.ViewStatsDTO;

import java.time.LocalDateTime;
import java.util.List;

@FeignClient(name = "stats-server", url = "${stats.service.url:http://localhost:9090}")
//@FeignClient(name = "stats-server")
public interface StatsFeignClient {

    @PostMapping("/hit")
    void createHit(@RequestBody EndpointHitDTO endpointHitDTO);

    @GetMapping("/stats")
    List<ViewStatsDTO> getStats(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end,
            @RequestParam(required = false) List<String> uris,
            @RequestParam(defaultValue = "false") Boolean unique);
}
