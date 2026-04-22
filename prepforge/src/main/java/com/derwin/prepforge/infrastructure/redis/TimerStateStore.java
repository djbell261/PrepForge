package com.derwin.prepforge.infrastructure.redis;

import java.util.Optional;
import java.util.UUID;

public interface TimerStateStore {
    void save(TimerState timerState);

    Optional<TimerState> find(UUID sessionId, TimedSessionType sessionType);

    void delete(UUID sessionId, TimedSessionType sessionType);
}
