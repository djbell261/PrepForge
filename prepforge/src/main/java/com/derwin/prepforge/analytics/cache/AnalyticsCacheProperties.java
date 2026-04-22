package com.derwin.prepforge.analytics.cache;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.analytics-cache")
public class AnalyticsCacheProperties {
    private boolean enabled = true;
    private String keyPrefix = "prepforge:analytics-cache";
    private long ttlSeconds = 120;
}
