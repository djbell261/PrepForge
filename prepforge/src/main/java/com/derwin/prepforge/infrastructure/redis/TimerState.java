package com.derwin.prepforge.infrastructure.redis;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimerState {
    private UUID sessionId;
    private UUID userId;
    private Instant expiresAt;
    private TimedSessionType sessionType;
}
