package com.derwin.prepforge.common.ratelimit;

import java.util.EnumMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.rate-limit.ai")
public class AiRateLimitProperties {
    private boolean enabled = true;
    private boolean failOpenOnRedisError = true;
    private String keyPrefix = "prepforge:rate-limit:ai";
    private int defaultLimit = 10;
    private long defaultWindowSeconds = 60;
    private Map<AiEndpointCategory, LimitConfig> categories = new EnumMap<>(AiEndpointCategory.class);

    public LimitConfig resolve(AiEndpointCategory category) {
        LimitConfig categoryConfig = categories.get(category);
        if (categoryConfig == null) {
            return new LimitConfig(defaultLimit, defaultWindowSeconds);
        }

        int resolvedLimit = categoryConfig.getLimit() != null ? categoryConfig.getLimit() : defaultLimit;
        long resolvedWindowSeconds = categoryConfig.getWindowSeconds() != null
                ? categoryConfig.getWindowSeconds()
                : defaultWindowSeconds;
        return new LimitConfig(resolvedLimit, resolvedWindowSeconds);
    }

    @Getter
    @Setter
    public static class LimitConfig {
        private Integer limit;
        private Long windowSeconds;

        public LimitConfig() {
        }

        public LimitConfig(Integer limit, Long windowSeconds) {
            this.limit = limit;
            this.windowSeconds = windowSeconds;
        }
    }
}
