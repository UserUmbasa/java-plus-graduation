package ru.practicum.eventservice.exception;

public class ConditionNotMetException extends RuntimeException {
    public ConditionNotMetException(String s) {
        super(s);
    }
}
