package com.derwin.prepforge.infrastructure.redis;

import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "false")
public class NoOpTimerStateStore implements TimerStateStore {
    @Override
    public void save(TimerState timerState) {
        // Redis is disabled for this environment, so timer projection is skipped.
    }

    @Override
    public Optional<TimerState> find(UUID sessionId, TimedSessionType sessionType) {
        return Optional.empty();
    }

    @Override
    public void delete(UUID sessionId, TimedSessionType sessionType) {
        // Redis is disabled for this environment, so timer projection is skipped.
    }
}
