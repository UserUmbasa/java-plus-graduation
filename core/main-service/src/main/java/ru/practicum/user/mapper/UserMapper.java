package ru.practicum.user.mapper;

import ru.practicum.user.dto.NewUserRequest;
import ru.practicum.user.dto.UserDtoOut;
import ru.practicum.user.model.User;

public class UserMapper {

    public static UserDtoOut toDto(User user) {
        return UserDtoOut.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }

    public static User toEntity(NewUserRequest request) {
        return User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .build();
    }
}