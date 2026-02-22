package ru.practicum.aggregator;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.practicum.aggregator.kafka.KafkaClient;
import ru.practicum.aggregator.service.AggregatorService;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

import java.time.Duration;
import java.util.List;
import java.util.Collections;

@Slf4j
@Component
public class AggregationStarter implements Runnable {

    private final Producer<Long, SpecificRecordBase> producer;
    private final Consumer<Long, SpecificRecordBase> consumer;
    private final AggregatorService aggregatorService;

    // Флаг для управления циклом
    private volatile boolean running = true;

    @Value("${kafka.topics.user-actions}")
    private String userActionsTopic;
    @Value("${kafka.topics.events-similarity}")
    private String eventsSimilarityTopic;

    private static final Duration POLL_TIMEOUT = Duration.ofMillis(1000);

    public AggregationStarter(KafkaClient kafkaClient, AggregatorService aggregatorService) {
        this.producer = kafkaClient.getProducer();
        this.consumer = kafkaClient.getConsumer();
        this.aggregatorService = aggregatorService;
    }

    @Override
    public void run() {
        log.info("AggregationStarter запущен");
        try {
            consumer.subscribe(Collections.singletonList(userActionsTopic));

            while (running) {
                ConsumerRecords<Long, SpecificRecordBase> records = consumer.poll(POLL_TIMEOUT);

                if (records.isEmpty()) {
                    continue;
                }

                for (ConsumerRecord<Long, SpecificRecordBase> record : records) {
                    try {
                        List<EventSimilarityAvro> eventsSimilarity = aggregatorService.aggregationUserAction(record.value());
                        if (!eventsSimilarity.isEmpty()) {
                            sendInProducer(eventsSimilarity);
                        }
                    } catch (Exception e) {
                        log.error("Ошибка при обработке конкретной записи: {}", record.offset(), e);
                    }
                }

                consumer.commitAsync((offsets, exception) -> {
                    if (exception != null) {
                        log.warn("Ошибка при асинхронной фиксации оффсетов батча: {}", exception.getMessage());
                    } else {
                        log.trace("Оффсеты батча успешно зафиксированы: {}", offsets);
                    }
                });
            }
        } catch (WakeupException ignored) {
            log.info("AggregationStarter: получен сигнал остановки (WakeupException)");
        } catch (Exception e) {
            log.error("AggregationStarter: Критическая ошибка в цикле обработки", e);
        } finally {
            log.info("AggregationStarter: Цикл обработки завершен");
            // consumer.close() сделает KafkaClientImpl
        }
    }

    @PreDestroy
    public void stop() {
        log.info("AggregationStarter: Инициирована остановка...");
        this.running = false;
        consumer.wakeup();
    }

    private void sendInProducer(List<EventSimilarityAvro> eventsSimilarity) {
        for (EventSimilarityAvro sim : eventsSimilarity) {
            producer.send(new ProducerRecord<>(
                    eventsSimilarityTopic,
                    null,
                    sim.getTimestamp().toEpochMilli(),
                    sim.getEventA(),
                    sim
            ), (metadata, ex) -> {
                if (ex != null) {
                    log.error("Ошибка при отправке в Producer: {}", ex.getMessage());
                }
            });
        }
    }
}