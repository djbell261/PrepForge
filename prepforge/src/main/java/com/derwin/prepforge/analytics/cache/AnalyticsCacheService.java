package com.derwin.prepforge.analytics.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsCacheService {
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final AnalyticsCacheProperties analyticsCacheProperties;

    public <T> T getOrLoad(UUID userId, AnalyticsCacheType cacheType, Class<T> targetType, Supplier<T> loader) {
        if (!analyticsCacheProperties.isEnabled() || userId == null || cacheType == null) {
            return loader.get();
        }

        String key = buildKey(userId, cacheType);

        try {
            String cachedValue = stringRedisTemplate.opsForValue().get(key);
            if (cachedValue != null && !cachedValue.isBlank()) {
                return objectMapper.readValue(cachedValue, targetType);
            }
        } catch (JsonProcessingException exception) {
            log.warn("Failed to deserialize analytics cache for key {}", key, exception);
        } catch (DataAccessException exception) {
            // Fail open and recompute from PostgreSQL-backed services when Redis is unavailable.
            log.warn("Redis unavailable while reading analytics cache for key {}", key, exception);
            return loader.get();
        }

        T loadedValue = loader.get();
        put(userId, cacheType, loadedValue);
        return loadedValue;
    }

    public void evict(UUID userId, AnalyticsCacheType cacheType) {
        if (!analyticsCacheProperties.isEnabled() || userId == null || cacheType == null) {
            return;
        }

        try {
            stringRedisTemplate.delete(buildKey(userId, cacheType));
        } catch (DataAccessException exception) {
            log.warn("Redis unavailable while evicting analytics cache for user {} and type {}", userId, cacheType, exception);
        }
    }

    public void evictAllAnalytics(UUID userId) {
        evict(userId, AnalyticsCacheType.CODING_SUMMARY);
        evict(userId, AnalyticsCacheType.BEHAVIORAL_SUMMARY);
        evict(userId, AnalyticsCacheType.DASHBOARD_COACHING_SUMMARY);
    }

    private void put(UUID userId, AnalyticsCacheType cacheType, Object value) {
        if (value == null) {
            return;
        }

        try {
            stringRedisTemplate.opsForValue().set(
                    buildKey(userId, cacheType),
                    objectMapper.writeValueAsString(value),
                    Duration.ofSeconds(Math.max(analyticsCacheProperties.getTtlSeconds(), 1)));
        } catch (JsonProcessingException exception) {
            log.warn("Failed to serialize analytics cache for user {} and type {}", userId, cacheType, exception);
        } catch (DataAccessException exception) {
            log.warn("Redis unavailable while writing analytics cache for user {} and type {}", userId, cacheType, exception);
        }
    }

    private String buildKey(UUID userId, AnalyticsCacheType cacheType) {
        return analyticsCacheProperties.getKeyPrefix() + ":" + cacheType.getKey() + ":" + userId;
    }
}
