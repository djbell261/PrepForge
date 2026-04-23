package com.derwin.prepforge.common.ratelimit;

import com.derwin.prepforge.infrastructure.observability.PrepForgeMetrics;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiRateLimitService {
    private final StringRedisTemplate stringRedisTemplate;
    private final AiRateLimitProperties aiRateLimitProperties;
    private final PrepForgeMetrics prepForgeMetrics;

    public void checkLimit(UUID userId, AiEndpointCategory endpointCategory) {
        if (!aiRateLimitProperties.isEnabled() || userId == null || endpointCategory == null) {
            return;
        }

        AiRateLimitProperties.LimitConfig config = aiRateLimitProperties.resolve(endpointCategory);
        if (config.getLimit() == null || config.getLimit() <= 0 || config.getWindowSeconds() == null || config.getWindowSeconds() <= 0) {
            return;
        }

        long nowEpochSeconds = Instant.now().getEpochSecond();
        long windowSeconds = config.getWindowSeconds();
        long windowStartEpoch = nowEpochSeconds - (nowEpochSeconds % windowSeconds);
        long retryAfterSeconds = Math.max((windowStartEpoch + windowSeconds) - nowEpochSeconds, 1);
        String key = buildKey(userId, endpointCategory, windowStartEpoch);

        try {
            Long currentCount = stringRedisTemplate.opsForValue().increment(key);
            if (currentCount == null) {
                return;
            }

            if (currentCount == 1L) {
                stringRedisTemplate.expire(key, java.time.Duration.ofSeconds(windowSeconds));
            }

            if (currentCount > config.getLimit()) {
                prepForgeMetrics.incrementRateLimitHit(endpointCategory.getKey());
                throw new AiRateLimitExceededException(endpointCategory, retryAfterSeconds);
            }
        } catch (DataAccessException exception) {
            if (aiRateLimitProperties.isFailOpenOnRedisError()) {
                // Fail open when Redis is unavailable so local dev and degraded environments remain usable.
                log.warn("Redis unavailable during AI rate-limit check for user {} and category {}", userId, endpointCategory, exception);
                return;
            }

            throw exception;
        }
    }

    private String buildKey(UUID userId, AiEndpointCategory endpointCategory, long windowStartEpoch) {
        return aiRateLimitProperties.getKeyPrefix()
                + ":" + endpointCategory.getKey()
                + ":" + userId
                + ":" + windowStartEpoch;
    }
}
