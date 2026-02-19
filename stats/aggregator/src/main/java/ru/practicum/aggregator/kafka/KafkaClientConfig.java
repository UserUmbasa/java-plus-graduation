package ru.practicum.aggregator.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Properties;

@Slf4j
@Configuration
public class KafkaClientConfig {
    @Bean
    @ConfigurationProperties(prefix = "kafka.producer.properties")
    public Properties kafkaProducerProperties() {
        log.info("{}: Создание Properties -> Producer", KafkaClientConfig.class.getSimpleName());
        return new Properties();
    }

    @Bean
    @ConfigurationProperties(prefix = "kafka.consumer.properties")
    public Properties kafkaConsumerProperties() {
        log.info("{}: Создание Properties -> Consumer", KafkaClientConfig.class.getSimpleName());
        return new Properties();
    }

    @Bean
    KafkaClient getKafkaClient() {
        return new KafkaClient() {
            private Producer<Long, SpecificRecordBase> kafkaProducer;
            private Consumer<Long, SpecificRecordBase> kafkaConsumer;

            @Override
            public Producer<Long, SpecificRecordBase> getProducer() {
                log.info("{}: Создание Producer", KafkaClientConfig.class.getSimpleName());
                kafkaProducer = new KafkaProducer<>(kafkaProducerProperties());
                return kafkaProducer;
            }

            @Override
            public Consumer<Long, SpecificRecordBase> getConsumer() {
                log.info("{}: Создание Consumer", KafkaClientConfig.class.getSimpleName());
                kafkaConsumer = new KafkaConsumer<>(kafkaConsumerProperties());
                return kafkaConsumer;
            }

            @Override
            public void close() {
                try {
                    kafkaProducer.flush();
                    kafkaConsumer.commitSync();
                } finally {
                    log.info("{}: Закрытие {}", Producer.class.getSimpleName(), KafkaClientConfig.class.getSimpleName());
                    kafkaProducer.close(Duration.ofSeconds(10));
                    log.info("{}: Закрытие {}", Consumer.class.getSimpleName(), KafkaClientConfig.class.getSimpleName());
                    kafkaConsumer.close();
                }
            }
        };
    }
}
