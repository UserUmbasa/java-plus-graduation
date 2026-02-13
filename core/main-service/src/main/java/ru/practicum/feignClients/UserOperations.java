package ru.practicum.feignClients;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.comment.dto.user.NewUserRequest;
import ru.practicum.comment.dto.user.UserDtoOut;

import java.util.List;

@Validated
@FeignClient(name = "user-service", path = "/admin/users")
public interface UserOperations {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    UserDtoOut createUser(@RequestBody @Valid NewUserRequest request);

    @GetMapping
    List<UserDtoOut> getUsers(@RequestParam(required = false) List<Long> ids,
                              @RequestParam(defaultValue = "0") @Min(0) int from,
                              @RequestParam(defaultValue = "10") @Min(1) int size);

    @GetMapping("/{userId}")
    UserDtoOut getUser(@PathVariable Long userId);

    @GetMapping("/{userId}/exists")
    boolean getExistsById(@PathVariable Long userId);

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteUser(@PathVariable Long userId);

}

