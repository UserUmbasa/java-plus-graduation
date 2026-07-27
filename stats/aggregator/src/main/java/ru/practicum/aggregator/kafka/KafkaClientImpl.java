package ru.practicum.aggregator.kafka;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.DisposableBean; // Используем для корректного закрытия
import java.time.Duration;
import java.util.Properties;

@Slf4j
public class KafkaClientImpl implements KafkaClient, DisposableBean {
    private final Producer<Long, SpecificRecordBase> kafkaProducer;
    private final Consumer<Long, SpecificRecordBase> kafkaConsumer;

    public KafkaClientImpl(
            Properties kafkaProducerProperties,
            Properties kafkaConsumerProperties) {
        this.kafkaProducer = new KafkaProducer<>(kafkaProducerProperties);
        this.kafkaConsumer = new KafkaConsumer<>(kafkaConsumerProperties);
        log.info("KafkaClientImpl: KafkaClient успешно инициализирован.");
    }

    @Override
    public Producer<Long, SpecificRecordBase> getProducer() {
        return kafkaProducer; // Возвращаем созданный синглтон-продюсер
    }

    @Override
    public Consumer<Long, SpecificRecordBase> getConsumer() {
        return kafkaConsumer; // Возвращаем созданный синглтон-консьюмер
    }

    @Override
    public void close() {
        log.info("KafkaClientImpl: Запрос на закрытие Kafka producer и consumer.");
        try {
            if (kafkaProducer != null) {
                kafkaProducer.flush();
                log.info("KafkaClientImpl: Producer flushed.");
            }
        } catch (Exception e) {
            log.warn("KafkaClientImpl: Ошибка при flush producer: {}", e.getMessage());
        }
        try {
            if (kafkaConsumer != null) {
                kafkaConsumer.commitSync();
                log.info("KafkaClientImpl: Consumer committed offsets synchronously.");
            }
        } catch (WakeupException e) {
            log.debug("KafkaClientImpl: Consumer commitSync interrupted by WakeupException during shutdown, which is expected.");
        } catch (Exception e) {
            log.warn("KafkaClientImpl: Ошибка при commitSync consumer: {}", e.getMessage());
        } finally {
            // закрываем оба клиента
            if (kafkaProducer != null) {
                try {
                    kafkaProducer.close(Duration.ofSeconds(10));
                    log.info("KafkaClientImpl: Producer закрыт.");
                } catch (Exception e) {
                    log.error("KafkaClientImpl: Ошибка при закрытии producer: {}", e.getMessage(), e);
                }
            }
            if (kafkaConsumer != null) {
                try {
                    kafkaConsumer.close();
                    log.info("KafkaClientImpl: Consumer закрыт.");
                } catch (Exception e) {
                    log.error("KafkaClientImpl: Ошибка при закрытии consumer: {}", e.getMessage(), e);
                }
            }
        }
        log.info("KafkaClientImpl: Kafka producer и consumer закрыты.");
    }

    @Override
    public void destroy() throws Exception {
        close();
    }
}
