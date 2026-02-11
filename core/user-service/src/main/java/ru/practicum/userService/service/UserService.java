package ru.practicum.userService.service;


import ru.practicum.userService.dto.NewUserRequest;
import ru.practicum.userService.dto.UserDtoOut;

import java.util.List;

public interface UserService {

    UserDtoOut createUser(NewUserRequest request);

    List<UserDtoOut> getUsers(List<Long> ids, int from, int size);

    UserDtoOut getUser(Long userId);

    void deleteUser(Long userId);

    boolean getExistsById(Long userId);
}