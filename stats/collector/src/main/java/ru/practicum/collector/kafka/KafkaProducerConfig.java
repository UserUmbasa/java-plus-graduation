package ru.practicum.collector.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Slf4j
@Component
public class KafkaProducerConfig implements KafkaClientProducer, DisposableBean {

    private Producer<String, SpecificRecordBase> producer;

    @Value("${kafka.bootstrap.server}")
    private String bootstrapServer;

    @Value("${kafka.value-serializer}")
    private String serializer;

    @Override
    public Producer<String, SpecificRecordBase> getProducer() {
        if (producer == null) {
            initProducer();
        }
        return producer;
    }

    private void initProducer() {
        Properties config = new Properties();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, serializer);

        producer = new KafkaProducer<>(config);
        log.info("Kafka Producer инициализирован.");
    }

    @Override
    public void stop() {
        if (producer != null) {
            producer.flush();
            producer.close();
            producer = null;
            log.info("Kafka Producer закрыт.");
        }
    }

    @Override
    public void destroy() throws Exception {
        stop();
    }
}
