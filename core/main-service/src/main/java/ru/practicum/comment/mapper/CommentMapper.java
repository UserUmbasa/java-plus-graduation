package ru.practicum.comment.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.comment.dto.CommentDto;
import ru.practicum.comment.dto.EventShortDtoOut;
import ru.practicum.comment.model.Comment;
import ru.practicum.feignClients.EventOperations;
import ru.practicum.feignClients.UserOperations;
import ru.practicum.participation.dto.event.EventDtoOut;

//@UtilityClass
@Component
@RequiredArgsConstructor
public class CommentMapper {

    private final UserOperations userOperations;
    private final EventOperations eventOperations;

    public CommentDto toDto(Comment comment) {
        EventDtoOut eventDtoOut = eventOperations.findById(comment.getEvent()).orElse(null);
        EventShortDtoOut eventShortDtoOut = EventShortDtoOut.builder()
                .id(eventDtoOut.getId())
                .title(eventDtoOut.getTitle())
                .paid(eventDtoOut.getPaid())
                .eventDate(eventDtoOut.getEventDate())
                .views(eventDtoOut.getViews())
                .confirmedRequests(eventDtoOut.getConfirmedRequests())
                .build();

        return CommentDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .event(eventShortDtoOut)
                .author(userOperations.getUser(comment.getUser()))
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .status(comment.getStatus().name())
                .build();
    }
}
