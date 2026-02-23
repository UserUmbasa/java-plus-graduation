package ru.practicum.collector.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Service;
import ru.practicum.collector.kafka.KafkaClientProducer;
import ru.practicum.collector.mapper.Mapper;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.proto.UserActionProto;

@Slf4j
@RequiredArgsConstructor
@Service
public class CollectorServiceImpl <T extends SpecificRecordBase> implements CollectorService{
    private final KafkaClientProducer producer;
    private final Mapper mapper;
    private final String TOPIC = "stats.user-actions.v1";

    @Override
    public void collectUserAction(UserActionProto request) {
        UserActionAvro userActionAvro = mapper.mapToAvro(request);
        ProducerRecord<Long, SpecificRecordBase> record = new ProducerRecord<>(
                TOPIC,
                null,
                userActionAvro);
        // отправка в кафку
        producer.getProducer().send(record);
        log.info("Отправили в Kafka: {}", request);
    }
}
