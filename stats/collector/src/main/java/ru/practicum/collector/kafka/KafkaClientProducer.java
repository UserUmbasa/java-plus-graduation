package ru.practicum.collector.kafka;


import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.Producer;

public interface KafkaClientProducer {
    Producer<Long, SpecificRecordBase> getProducer();

    void stop();
}
