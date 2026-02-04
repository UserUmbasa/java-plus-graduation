package ru.practicum;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.EndpointHitDTO;
import ru.practicum.dto.ViewStatsDTO;

import java.time.LocalDateTime;
import java.util.List;

//@FeignClient(name = "stats-server", path = "/stats-server")
@FeignClient(name = "stats-server", url = "localhost:9090")
//@Primary
public interface StatsApi {

    @PostMapping("/hit")
    public void createHit(@RequestBody EndpointHitDTO endpointHitDTO);

    @GetMapping("/stats")
    public List<ViewStatsDTO> getStats(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end,
            @RequestParam(required = false) List<String> uris,
            @RequestParam(defaultValue = "false") Boolean unique);
}

