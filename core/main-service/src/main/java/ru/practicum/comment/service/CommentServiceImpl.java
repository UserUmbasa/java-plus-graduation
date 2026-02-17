package ru.practicum.comment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.comment.dto.CommentCreateDto;
import ru.practicum.comment.dto.CommentDto;
import ru.practicum.comment.dto.CommentUpdateDto;
import ru.practicum.comment.dto.event.EventDtoOut;
import ru.practicum.comment.dto.event.EventState;
import ru.practicum.comment.mapper.CommentMapper;
import ru.practicum.comment.model.Comment;
import ru.practicum.comment.model.CommentStatus;
import ru.practicum.comment.repository.CommentRepository;
import ru.practicum.exception.ConditionNotMetException;
import ru.practicum.exception.NoAccessException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.feignClients.EventOperations;
import ru.practicum.feignClients.UserOperations;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;
    private final CommentRepository commentRepository;
    private final UserOperations userOperations;
    private final EventOperations eventOperations;

    @Override
    @Transactional
    public CommentDto createComment(Long userId, Long eventId, CommentCreateDto commentCreateDto) {
        log.info("Создание комментария пользователем {} к событию {}", userId, eventId);
        if (!userOperations.getExistsById(userId)) {
            throw new NotFoundException("User", userId);
        }
        EventDtoOut event = eventOperations.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event", eventId));
        if (event.getState() != EventState.PUBLISHED) {
            throw new ConditionNotMetException("Нельзя оставлять комментарии к неопубликованному событию");
        }
        Comment comment = Comment.builder()
                .text(commentCreateDto.getText().trim())
                .user(userId)
                .event(eventId)
                .status(CommentStatus.PUBLISHED)
                .build();

        Comment saved = commentRepository.save(comment);
        log.info("Создан комментарий ID: {} пользователем ID: {} к событию ID: {}",
                saved.getId(), userId, eventId);
        return commentMapper.toDto(saved);
    }

    @Override
    @Transactional
    public CommentDto updateComment(Long userId, Long commentId, CommentUpdateDto commentUpdateDto) {
        log.info("Обновление комментария {} пользователем {}", commentId, userId);
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment", commentId));
        if (!comment.getUser().equals(userId)) {
            throw new NoAccessException("Редактировать можно только свои комментарии");
        }
        if (comment.getStatus() == CommentStatus.DELETED) {
            throw new ConditionNotMetException("Нельзя редактировать удаленный комментарий");
        }
        if (commentUpdateDto.getText() != null && !commentUpdateDto.getText().trim().isEmpty()) {
            comment.setText(commentUpdateDto.getText().trim());
            comment.setStatus(CommentStatus.EDITED);
        }

        Comment updated = commentRepository.save(comment);
        return commentMapper.toDto(updated);
    }

    @Override
    @Transactional
    public void deleteCommentByUser(Long userId, Long commentId) {
        log.info("Удаление комментария {} пользователем {}", commentId, userId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment", commentId));

        if (!comment.getUser().equals(userId)) {
            throw new NoAccessException("Удалять можно только свои комментарии");
        }

        comment.setStatus(CommentStatus.DELETED);
        commentRepository.save(comment);
    }

    @Override
    @Transactional
    public void deleteCommentByAdmin(Long commentId) {
        log.info("Удаление комментария {} администратором", commentId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment", commentId));

        comment.setStatus(CommentStatus.DELETED);
        commentRepository.save(comment);
    }

    @Override
    public CommentDto getComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment", commentId));

        if (comment.getStatus() == CommentStatus.DELETED) {
            throw new NotFoundException("Comment", commentId);
        }

        return commentMapper.toDto(comment);
    }

    @Override
    public List<CommentDto> getEventComments(Long eventId, Pageable pageable) {
        log.info("Получение комментариев события {}", eventId);

        if (!eventOperations.getExistsById(eventId)) {
            throw new NotFoundException("Event", eventId);
        }

        List<CommentStatus> activeStatuses = List.of(CommentStatus.PUBLISHED, CommentStatus.EDITED);
        return commentRepository
                .findByEventAndStatusInOrderByCreatedAtDesc(eventId, activeStatuses, pageable)
                .getContent()
                .stream()
                .map(commentMapper::toDto)
                .toList();
    }

    @Override
    public List<CommentDto> getUserComments(Long userId, Pageable pageable) {
        log.info("Получение комментариев пользователя {}", userId);
        if (!userOperations.getExistsById(userId)) {
            throw new NotFoundException("User", userId);
        }
        return commentRepository
                .findByUserAndStatusNotOrderByCreatedAtDesc(userId, CommentStatus.DELETED, pageable)
                .getContent()
                .stream()
                .map(commentMapper::toDto)
                .toList();
    }

    @Override
    public List<CommentDto> getCommentsAdmin(List<Long> events, List<Long> users, Pageable pageable) {
        log.info("Получение комментариев админом events: {}, users: {}", events, users);

        return commentRepository
                .findByEventIdInAndUserIdInOrderByCreatedAtDesc(events, users, pageable)
                .getContent()
                .stream()
                .map(commentMapper::toDto)
                .toList();
    }
}