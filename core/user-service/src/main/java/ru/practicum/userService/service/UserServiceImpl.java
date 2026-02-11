package ru.practicum.userService.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.userService.dto.NewUserRequest;
import ru.practicum.userService.dto.UserDtoOut;
import ru.practicum.userService.mapper.UserMapper;
import ru.practicum.userService.model.User;
import ru.practicum.userService.repository.UserRepository;


import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserDtoOut createUser(NewUserRequest request) {
        User user = UserMapper.toEntity(request);
        return UserMapper.toDto(userRepository.save(user));
    }

    @Override
    public List<UserDtoOut> getUsers(List<Long> ids, int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size);
        List<User> users;
        if (ids == null || ids.isEmpty()) {
            users = userRepository.findUsers(null, pageable);
        } else {
            users = userRepository.findUsers(ids, pageable);
        }
        return users.stream().map(UserMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public UserDtoOut getUser(Long userId) {
        User result = userRepository.findById(userId).orElse(null);
        return UserMapper.toDto(result);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }

    @Override
    public boolean getExistsById(Long userId) {
        return userRepository.existsById(userId);
    }
}