package ru.practicum.analyzer.processor;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.practicum.analyzer.handler.EventSimilarityHandler;
import ru.practicum.analyzer.kafka.KafkaClient;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

import java.time.Duration;
import java.util.Collections;

@Slf4j
@Component
public class EventSimilarityProcessor implements Runnable {

    private final Consumer<Long, EventSimilarityAvro> similarityConsumer;
    private final EventSimilarityHandler similarityHandler;
    private final String eventsSimilarityTopic;

    private volatile boolean running = true;

    private static final Duration CONSUME_ATTEMPT_TIMEOUT = Duration.ofMillis(1000);

    public EventSimilarityProcessor(
            KafkaClient kafkaClient,
            EventSimilarityHandler similarityHandler,
            @Value("${analyzer.kafka.topics.events-similarity}") String eventsSimilarityTopic) {
        this.similarityConsumer = kafkaClient.getKafkaEventSimilarityConsumer();
        this.similarityHandler = similarityHandler;
        this.eventsSimilarityTopic = eventsSimilarityTopic;
    }

    @Override
    public void run() {
        log.info("EventSimilarityProcessor запущен.");
        try {
            similarityConsumer.subscribe(Collections.singletonList(eventsSimilarityTopic));

            while (running) {
                ConsumerRecords<Long, EventSimilarityAvro> records = similarityConsumer.poll(CONSUME_ATTEMPT_TIMEOUT);

                if (!records.isEmpty()) {
                    for (ConsumerRecord<Long, EventSimilarityAvro> record : records) {
                        try {
                            log.debug("Обработка сообщения: partition={}, offset={}, value={}",
                                    record.partition(), record.offset(), record.value());
                            similarityHandler.handleEventSimilarity(record.value());
                        } catch (Exception e) {
                            log.error("Ошибка при обработке конкретного события (offset {}): {}",
                                    record.offset(), e.getMessage());
                        }
                    }

                    similarityConsumer.commitAsync();
                }
            }
        } catch (WakeupException ignored) {
            log.info("EventSimilarityProcessor: получен сигнал остановки (Wakeup).");
        } catch (Exception e) {
            log.error("Критическая ошибка в EventSimilarityProcessor: {}", e.getMessage(), e);
        } finally {
            try {
                similarityConsumer.commitSync();
                log.info("Финальные оффсеты успешно зафиксированы.");
            } catch (Exception e) {
                log.error("Ошибка при финальной фиксации оффсетов: {}", e.getMessage());
            } finally {
                similarityConsumer.close();
                log.info("EventSimilarityProcessor: консьюмер закрыт.");
            }
        }
    }

    public void stop() {
        this.running = false;
        similarityConsumer.wakeup();
    }
}
