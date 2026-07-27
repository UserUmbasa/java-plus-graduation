package ru.practicum.analyzer.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.analyzer.mapper.EventSimilarityMapper;
import ru.practicum.analyzer.model.EventSimilarity;
import ru.practicum.analyzer.repository.EventSimilarityRepository;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventSimilarityHandlerImpl implements EventSimilarityHandler {
    private final EventSimilarityRepository similarityRepository;
    private final EventSimilarityMapper similarityMapper;

    @Override
    public void handleEventSimilarity(EventSimilarityAvro avro) {
        EventSimilarity similarity = similarityMapper.mapToEventSimilarity(avro);

        EventSimilarity oldSimilarity = similarityRepository
                .findByEventAAndEventB(similarity.getEventA(), similarity.getEventB())
                .orElse(null);
        if (oldSimilarity == null) {
            similarity = similarityRepository.save(similarity);
            log.info("Сохранить новое сходство: {}", similarity);
        } else {
            log.info("Найти в БД старое сходство: {}", oldSimilarity);
            if (similarity.getScore() > oldSimilarity.getScore()) {
                oldSimilarity.setScore(similarity.getScore());
                oldSimilarity.setTimestamp(similarity.getTimestamp());
                oldSimilarity = similarityRepository.save(oldSimilarity);
                log.info("Сходство увеличилось, обновить БД: {}", oldSimilarity);
            } else {
                log.info("Сходство не увеличилось, не обновлять");
            }
        }
    }
}
