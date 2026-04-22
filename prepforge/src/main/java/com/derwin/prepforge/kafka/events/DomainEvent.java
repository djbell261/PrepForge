package com.derwin.prepforge.kafka.events;

import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DomainEvent {
    private final String eventType;
    private final String aggregateId;
    private final Instant occurredAt;
}
