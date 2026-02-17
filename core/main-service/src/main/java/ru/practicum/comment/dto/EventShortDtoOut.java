package ru.practicum.comment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import ru.practicum.comment.dto.category.CategoryDtoOut;
import ru.practicum.comment.dto.user.UserDtoOut;

import java.time.LocalDateTime;

import static ru.practicum.constants.Constants.DATE_TIME_FORMAT;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventShortDtoOut {

    private Long id;
    private String title;
    private String annotation;
    private CategoryDtoOut category;
    private UserDtoOut initiator;

    @JsonFormat(pattern = DATE_TIME_FORMAT)
    private LocalDateTime eventDate;

    private Boolean paid;
    private Integer confirmedRequests;

    @Builder.Default
    private Long views = 0L;
}