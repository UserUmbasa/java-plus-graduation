package ru.practicum.userService.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.userService.dto.NewUserRequest;
import ru.practicum.userService.dto.UserDtoOut;
import ru.practicum.userService.service.UserService;


import java.util.List;

@RestController
@Validated
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDtoOut createUser(@RequestBody @Valid NewUserRequest request) {
        return userService.createUser(request);
    }

    @GetMapping
    public List<UserDtoOut> getUsers(@RequestParam(required = false) List<Long> ids,
                                     @RequestParam(defaultValue = "0") @Min(0) int from,
                                     @RequestParam(defaultValue = "10") @Min(1) int size) {
        return userService.getUsers(ids, from, size);
    }

    @GetMapping("/ids")
    public List<UserDtoOut> getUsers(@RequestParam List<Long> ids) {
        return userService.getUsers(ids);
    }

    @GetMapping("/{userId}")
    public UserDtoOut getUser(@PathVariable Long userId) {
        return userService.getUser(userId);
    }

    @GetMapping("/{userId}/exists")
    public boolean getExistsById(@PathVariable Long userId) {
        return userService.getExistsById(userId);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
    }
}