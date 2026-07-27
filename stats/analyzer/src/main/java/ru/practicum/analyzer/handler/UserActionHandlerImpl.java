package ru.practicum.analyzer.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.analyzer.mapper.UserActionMapper;
import ru.practicum.analyzer.model.UserAction;
import ru.practicum.analyzer.repository.UserActionRepository;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionHandlerImpl implements UserActionHandler {

    private final UserActionRepository actionRepository;
    private final UserActionMapper actionMapper;

    @Override
    public void handleUserAction(UserActionAvro avro) {
        UserAction action = actionMapper.mapToUserAction(avro);

        UserAction oldAction = actionRepository
                .findByEventIdAndUserId(action.getEventId(), action.getUserId())
                .orElse(null);

        if (oldAction == null) {
            action = actionRepository.save(action);
            log.info("Сохранить новое действие: {}", action);
        } else {
            log.info("Найти в БД старое действие: {}", oldAction);
            if (action.getWeight() > oldAction.getWeight()) {
                oldAction.setWeight(action.getWeight());
                oldAction.setTimestamp(action.getTimestamp());
                oldAction = actionRepository.save(oldAction);
                log.info("Вес действия увеличился, обновить БД: {}", oldAction);
            } else {
                log.info("Вес действия не увеличился, не обновлять");
            }
        }
    }
}
