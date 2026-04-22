package com.derwin.prepforge.kafka.producers;

import com.derwin.prepforge.kafka.events.DomainEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DomainEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(String topic, DomainEvent event) {
        kafkaTemplate.send(topic, event);
    }
}
