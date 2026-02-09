package ru.practicum.interactionApi.api.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.interactionApi.dto.user.NewUserRequest;
import ru.practicum.interactionApi.dto.user.UserDtoOut;

import java.util.List;

@FeignClient(name = "user-service")
public interface UserOperations {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDtoOut createUser(@RequestBody @Valid NewUserRequest request);

    @GetMapping
    public List<UserDtoOut> getUsers(@RequestParam(required = false) List<Long> ids,
                                     @RequestParam(defaultValue = "0") @Min(0) int from,
                                     @RequestParam(defaultValue = "10") @Min(1) int size);

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long userId);
}
