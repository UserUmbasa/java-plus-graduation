package ru.practicum.requestservice.exception;

public class NoAccessException extends RuntimeException {
    public NoAccessException(String s) {
        super(s);
    }
}
