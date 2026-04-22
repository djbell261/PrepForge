package com.derwin.prepforge.infrastructure.redis;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.timer-state")
public class TimerStateProperties {
    private String keyPrefix = "prepforge:timed-session";
    private long ttlGraceSeconds = 30;
}
