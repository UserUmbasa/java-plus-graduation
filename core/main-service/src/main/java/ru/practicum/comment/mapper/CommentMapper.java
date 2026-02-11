package ru.practicum.comment.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.comment.dto.CommentDto;
import ru.practicum.comment.model.Comment;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.feignClients.UserOperations;

//@UtilityClass
@Component
@RequiredArgsConstructor
public class CommentMapper {

    private final EventMapper eventMapper;
    private final UserOperations userOperations;

    public CommentDto toDto(Comment comment) {
        return CommentDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .event(eventMapper.toShortDto(comment.getEvent()))
                .author(userOperations.getUser(comment.getUser()))
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .status(comment.getStatus().name())
                .build();
    }
}
