package ru.practicum.aggregator.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.Properties;

@Slf4j
@Configuration
public class KafkaClientConfig {
    @Bean
    @ConfigurationProperties(prefix = "kafka.producer.properties")
    public Properties kafkaProducerProperties() {
        return new Properties();
    }

    @Bean
    @ConfigurationProperties(prefix = "kafka.consumer.properties")
    public Properties kafkaConsumerProperties() {
        return new Properties();
    }

    @Bean
    public KafkaClient getKafkaClient( Properties kafkaProducerProperties, Properties kafkaConsumerProperties) {
        return new KafkaClientImpl(kafkaProducerProperties, kafkaConsumerProperties);
    }
}