package ru.practicum.comment.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.comment.dto.CommentDto;
import ru.practicum.comment.model.Comment;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.user.mapper.UserMapper;

@UtilityClass
public class CommentMapper {
    public static CommentDto toDto(Comment comment) {
        return CommentDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .event(EventMapper.toShortDto(comment.getEvent()))
                .author(UserMapper.toDto(comment.getUser()))
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .status(comment.getStatus().name())
                .build();
    }
}
