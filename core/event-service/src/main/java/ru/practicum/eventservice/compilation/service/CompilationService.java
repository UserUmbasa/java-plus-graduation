package ru.practicum.eventservice.compilation.service;

import ru.practicum.eventservice.compilation.dto.CompilationDto;
import ru.practicum.eventservice.compilation.dto.NewCompilationDto;
import ru.practicum.eventservice.compilation.dto.UpdateCompilationRequest;

import java.util.List;

public interface CompilationService {
    CompilationDto getCompilationById(Long compId);

    List<CompilationDto> getCompilations(Boolean pinned, int from, int size);

    void deleteCompilation(Long compId);

    CompilationDto createCompilation(NewCompilationDto newCompilationDto);

    CompilationDto updateCompilation(Long compId, UpdateCompilationRequest dto);
}