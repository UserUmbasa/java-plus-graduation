package ru.practicum.collector.service;
import ru.practicum.ewm.stats.proto.UserActionProto;

public interface CollectorService {
    public void collectUserAction(UserActionProto request);
}
