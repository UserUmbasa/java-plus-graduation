package ru.practicum.eventservice.compilation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import ru.practicum.eventservice.compilation.dto.CompilationDto;
import ru.practicum.eventservice.compilation.dto.NewCompilationDto;
import ru.practicum.eventservice.compilation.dto.UpdateCompilationRequest;
import ru.practicum.eventservice.compilation.mapper.CompilationMapper;
import ru.practicum.eventservice.compilation.model.Compilation;
import ru.practicum.eventservice.compilation.repository.CompilationRepository;
import ru.practicum.eventservice.event.dto.EventShortDtoOut;
import ru.practicum.eventservice.event.dto.user.UserDtoOut;
import ru.practicum.eventservice.event.mapper.EventMapper;
import ru.practicum.eventservice.event.model.Event;
import ru.practicum.eventservice.event.repository.EventRepository;
import ru.practicum.eventservice.exception.ConditionNotMetException;
import ru.practicum.eventservice.exception.NotFoundException;
import ru.practicum.eventservice.feignClients.UserOperations;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompilationServiceImpl implements CompilationService {

    private final CompilationMapper compilationMapper;
    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final UserOperations userOperations;
    private final TransactionTemplate transactionTemplate;

    @Override
    public List<CompilationDto> getCompilations(Boolean pinned, int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size);
        List<Compilation> compilations = (pinned != null)
                ? compilationRepository.findByPinned(pinned, pageable)
                : compilationRepository.findAll(pageable).getContent();
        Set<Event> allEvents = compilations.stream()
                .flatMap(c -> c.getEvents().stream())
                .collect(Collectors.toSet());
        Map<Long, UserDtoOut> globalUsersMap = getUsersMap(allEvents);
        return compilations.stream()
                .map(compilation -> {
                    List<EventShortDtoOut> eventShortDtos = compilation.getEvents().stream()
                            .map(event -> eventMapper.toShortDto(event, globalUsersMap.get(event.getInitiator())))
                            .collect(Collectors.toList());

                    return compilationMapper.toDto(compilation, eventShortDtos);
                })
                .collect(Collectors.toList());
    }

    @Override
    public CompilationDto getCompilationById(Long compId) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation", compId));
        Set<Event> events = compilation.getEvents();
        Map<Long, UserDtoOut> usersMap = getUsersMap(events);
        List<EventShortDtoOut> eventShortDtos = events.stream()
                .map(event -> {
                    UserDtoOut initiator = usersMap.get(event.getInitiator());
                    return eventMapper.toShortDto(event, initiator);
                })
                .collect(Collectors.toList());
        return compilationMapper.toDto(compilation, eventShortDtos);
    }

    public CompilationDto createCompilation(NewCompilationDto newCompilationDto) {
        Compilation saved = transactionTemplate.execute(status -> {
            if (compilationRepository.existsByTitle(newCompilationDto.getTitle())) {
                throw new ConditionNotMetException("A compilation with this title already exists");
            }
            Set<Event> events = new HashSet<>();
            if (newCompilationDto.getEvents() != null && !newCompilationDto.getEvents().isEmpty()) {
                events = new HashSet<>(eventRepository.findAllById(newCompilationDto.getEvents()));
            }
            Compilation compilation = compilationMapper.toEntity(newCompilationDto, events);
            return compilationRepository.save(compilation);
        });
        Set<Event> events = saved.getEvents();
        Map<Long, UserDtoOut> usersMap = getUsersMap(events);
        List<EventShortDtoOut> eventShortDtos = events.stream()
                .map(event -> eventMapper.toShortDto(event, usersMap.get(event.getInitiator())))
                .collect(Collectors.toList());
        return compilationMapper.toDto(saved, eventShortDtos);
    }

    @Transactional
    @Override
    public void deleteCompilation(Long compId) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation", compId));
        compilationRepository.delete(compilation);
    }

    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequest dto) {
        Compilation saved = transactionTemplate.execute(status -> {
            Compilation compilation = compilationRepository.findById(compId)
                    .orElseThrow(() -> new NotFoundException("Compilation", compId));
            if (dto.getTitle() != null) {
                compilation.setTitle(dto.getTitle());
            }
            if (dto.getPinned() != null) {
                compilation.setPinned(dto.getPinned());
            }
            if (dto.getEvents() != null) {
                Set<Event> newEvents = new HashSet<>(eventRepository.findAllById(dto.getEvents()));
                compilation.setEvents(newEvents);
            }
            return compilationRepository.save(compilation);
        });
        Set<Event> events = saved.getEvents();
        Map<Long, UserDtoOut> usersMap = getUsersMap(events);
        List<EventShortDtoOut> eventShortDtos = events.stream()
                .map(event -> eventMapper.toShortDto(event, usersMap.get(event.getInitiator())))
                .collect(Collectors.toList());
        return compilationMapper.toDto(saved, eventShortDtos);
    }

    private Map<Long, UserDtoOut> getUsersMap(Collection<Event> events) {
        if (events.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> userIds = events.stream()
                .map(Event::getInitiator)
                .distinct()
                .collect(Collectors.toList());
        return userOperations.getUsers(userIds)
                .stream()
                .collect(Collectors.toMap(UserDtoOut::getId, user -> user));
    }
}