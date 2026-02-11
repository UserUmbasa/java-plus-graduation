package ru.practicum.userService.mapper;


import ru.practicum.userService.dto.NewUserRequest;
import ru.practicum.userService.dto.UserDtoOut;
import ru.practicum.userService.model.User;

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