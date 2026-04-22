package com.derwin.prepforge.analytics.cache;

public enum AnalyticsCacheType {
    CODING_SUMMARY("coding-summary"),
    BEHAVIORAL_SUMMARY("behavioral-summary");

    private final String key;

    AnalyticsCacheType(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
