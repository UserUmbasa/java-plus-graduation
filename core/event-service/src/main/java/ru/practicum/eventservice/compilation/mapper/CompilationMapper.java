package ru.practicum.eventservice.compilation.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.eventservice.compilation.dto.CompilationDto;
import ru.practicum.eventservice.compilation.dto.NewCompilationDto;
import ru.practicum.eventservice.compilation.model.Compilation;
import ru.practicum.eventservice.event.dto.event.EventShortDtoOut;
import ru.practicum.eventservice.event.model.Event;
import java.util.*;

@Component
@RequiredArgsConstructor
public class CompilationMapper {

    public Compilation toEntity(NewCompilationDto dto, Set<Event> events) {
        return Compilation.builder()
                .title(dto.getTitle())
                .pinned(dto.getPinned() != null && dto.getPinned())
                .events(events)
                .build();
    }

    public CompilationDto toDto(Compilation compilation ,List<EventShortDtoOut> eventShortDtoOut) {
        return CompilationDto.builder()
                .id(compilation.getId())
                .title(compilation.getTitle())
                .pinned(compilation.getPinned())
                .events(eventShortDtoOut)
                .build();
    }
}