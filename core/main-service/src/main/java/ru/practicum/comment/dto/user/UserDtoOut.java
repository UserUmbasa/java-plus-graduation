package ru.practicum.comment.dto.user;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDtoOut {
    private Long id;
    private String name;
    private String email;
}