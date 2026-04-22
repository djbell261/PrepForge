package com.derwin.prepforge.config;

import com.derwin.prepforge.infrastructure.redis.TimerStateProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TimerStateProperties.class)
public class AppConfig {
}
