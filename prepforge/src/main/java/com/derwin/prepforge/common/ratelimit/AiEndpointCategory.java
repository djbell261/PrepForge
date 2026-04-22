package com.derwin.prepforge.common.ratelimit;

public enum AiEndpointCategory {
    CODING_FEEDBACK("coding_feedback"),
    CODING_IMPROVEMENT("coding_improvement"),
    STRATEGY_EVALUATION("strategy_evaluation"),
    APPROACH_COMPARISON("approach_comparison"),
    BEHAVIORAL_FEEDBACK("behavioral_feedback"),
    BEHAVIORAL_IMPROVEMENT("behavioral_improvement");

    private final String key;

    AiEndpointCategory(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
