package ru.practicum.analyzer.processor;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.practicum.analyzer.handler.UserActionHandler;
import ru.practicum.analyzer.kafka.KafkaClient;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import java.time.Duration;
import java.util.Collections;

@Slf4j
@Component
public class UserActionProcessor implements Runnable {

    private final Consumer<Long, UserActionAvro> userActionConsumer;
    private final UserActionHandler userActionHandler;
    private final String userActionsTopic;

    // Флаг остановки цикла
    private volatile boolean running = true;

    private static final Duration CONSUME_ATTEMPT_TIMEOUT = Duration.ofMillis(1000);

    public UserActionProcessor(
            KafkaClient kafkaClient,
            UserActionHandler userActionHandler,
            @Value("${analyzer.kafka.topics.user-actions}") String userActionsTopic) {
        this.userActionConsumer = kafkaClient.getKafkaUserActionConsumer();
        this.userActionHandler = userActionHandler;
        this.userActionsTopic = userActionsTopic;
    }

    @Override
    public void run() {
        log.info("UserActionProcessor запущен.");
        try {
            userActionConsumer.subscribe(Collections.singletonList(userActionsTopic));
            while (running) {
                // Получаем пакет сообщений
                ConsumerRecords<Long, UserActionAvro> records = userActionConsumer.poll(CONSUME_ATTEMPT_TIMEOUT);

                if (!records.isEmpty()) {
                    for (ConsumerRecord<Long, UserActionAvro> record : records) {
                        try {
                            UserActionAvro avro = record.value();
                            log.debug("Обработка сообщения: partition={}, offset={}, value={}",
                                    record.partition(), record.offset(), avro);
                            // Обрабатываем действие пользователя
                            userActionHandler.handleUserAction(avro);

                        } catch (Exception e) {
                            log.error("Ошибка при обработке сообщения (partition={}, offset={}): {}",
                                    record.partition(), record.offset(), e.getMessage(), e);
                        }
                    }
                    userActionConsumer.commitAsync();
                }
            }
        } catch (WakeupException e) {
            log.info("UserActionProcessor получил WakeupException, инициируя штатное завершение.");
        } catch (Exception e) {
            log.error("Критическая ошибка в основном цикле UserActionProcessor: {}", e.getMessage(), e);
        } finally {
            try {
                userActionConsumer.commitSync();
                log.info("UserActionProcessor: финальная синхронная фиксация оффсетов выполнена.");
            } catch (Exception e) {
                log.error("UserActionProcessor: ошибка при финальной синхронной фиксации оффсетов: {}", e.getMessage(), e);
            } finally {
                userActionConsumer.close();
                log.info("UserActionProcessor: консьюмер Kafka закрыт.");
            }
        }
        log.info("UserActionProcessor завершил работу.");
    }

    public void stop() {
        this.running = false;
        userActionConsumer.wakeup();
    }
}
