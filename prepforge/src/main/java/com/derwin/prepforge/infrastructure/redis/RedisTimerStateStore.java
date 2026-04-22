package com.derwin.prepforge.infrastructure.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true", matchIfMissing = true)
public class RedisTimerStateStore implements TimerStateStore {
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final TimerStateProperties timerStateProperties;

    @Override
    public void save(TimerState timerState) {
        if (timerState == null || timerState.getSessionId() == null || timerState.getExpiresAt() == null
                || timerState.getSessionType() == null) {
            return;
        }

        Duration ttl = computeTtl(timerState.getExpiresAt());
        if (ttl.isZero() || ttl.isNegative()) {
            return;
        }

        try {
            stringRedisTemplate.opsForValue()
                    .set(buildKey(timerState.getSessionId(), timerState.getSessionType()), write(timerState), ttl);
        } catch (JsonProcessingException exception) {
            log.warn("Failed to serialize timer state for session {}", timerState.getSessionId(), exception);
        } catch (DataAccessException exception) {
            log.warn("Redis unavailable while saving timer projection for session {}", timerState.getSessionId(), exception);
        }
    }

    @Override
    public Optional<TimerState> find(UUID sessionId, TimedSessionType sessionType) {
        try {
            String payload = stringRedisTemplate.opsForValue().get(buildKey(sessionId, sessionType));
            if (payload == null || payload.isBlank()) {
                return Optional.empty();
            }

            return Optional.of(objectMapper.readValue(payload, TimerState.class));
        } catch (JsonProcessingException exception) {
            log.warn("Failed to deserialize timer state for session {}", sessionId, exception);
            return Optional.empty();
        } catch (DataAccessException exception) {
            log.warn("Redis unavailable while reading timer projection for session {}", sessionId, exception);
            return Optional.empty();
        }
    }

    @Override
    public void delete(UUID sessionId, TimedSessionType sessionType) {
        try {
            stringRedisTemplate.delete(buildKey(sessionId, sessionType));
        } catch (DataAccessException exception) {
            log.warn("Redis unavailable while deleting timer projection for session {}", sessionId, exception);
        }
    }

    private String buildKey(UUID sessionId, TimedSessionType sessionType) {
        return timerStateProperties.getKeyPrefix() + ":" + sessionType.name().toLowerCase() + ":" + sessionId;
    }

    private String write(TimerState timerState) throws JsonProcessingException {
        return objectMapper.writeValueAsString(timerState);
    }

    private Duration computeTtl(Instant expiresAt) {
        Duration untilExpiry = Duration.between(Instant.now(), expiresAt);
        Duration grace = Duration.ofSeconds(Math.max(timerStateProperties.getTtlGraceSeconds(), 0));
        return untilExpiry.plus(grace);
    }
}
