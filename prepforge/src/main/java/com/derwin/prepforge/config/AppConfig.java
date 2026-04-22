package com.derwin.prepforge.config;

import com.derwin.prepforge.analytics.cache.AnalyticsCacheProperties;
import com.derwin.prepforge.common.ratelimit.AiRateLimitProperties;
import com.derwin.prepforge.infrastructure.redis.TimerStateProperties;
import com.derwin.prepforge.jobs.AsyncJobProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties({
        TimerStateProperties.class,
        AiRateLimitProperties.class,
        AnalyticsCacheProperties.class,
        AsyncJobProperties.class
})
public class AppConfig {
}
