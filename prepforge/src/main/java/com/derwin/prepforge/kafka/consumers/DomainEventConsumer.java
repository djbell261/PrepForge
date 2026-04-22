package com.derwin.prepforge.kafka.consumers;

import com.derwin.prepforge.kafka.events.DomainEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DomainEventConsumer {

    @KafkaListener(topics = "prepforge.events", groupId = "prepforge")
    public void consume(DomainEvent event) {
        log.debug("Consumed event type={}", event.getEventType());
    }
}
