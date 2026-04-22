package com.derwin.prepforge.kafka.events;

import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserRegisteredEvent {
    private final String userId;
    private final String email;
    private final Instant occurredAt;
}
