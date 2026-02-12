package ru.practicum.eventservice.feignClients;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.eventservice.event.dto.user.NewUserRequest;
import ru.practicum.eventservice.event.dto.user.UserDtoOut;

import java.util.List;

//@FeignClient(name = "user-service")
@Validated
@FeignClient(name = "user-service", path = "/admin/users")
public interface UserOperations {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDtoOut createUser(@RequestBody @Valid NewUserRequest request);

    @GetMapping
    public List<UserDtoOut> getUsers(@RequestParam(required = false) List<Long> ids,
                                     @RequestParam(defaultValue = "0") @Min(0) int from,
                                     @RequestParam(defaultValue = "10") @Min(1) int size);

    @GetMapping("/{userId}")
    public UserDtoOut getUser(@PathVariable Long userId);

    @GetMapping("/{userId}/exists")
    public boolean getExistsById(@PathVariable Long userId);

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long userId);
}

