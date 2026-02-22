package ru.practicum.aggregator.kafka;

import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.producer.Producer;

public interface KafkaClient extends AutoCloseable {
    Producer<Long, SpecificRecordBase> getProducer();
    Consumer<Long, SpecificRecordBase> getConsumer();
}
