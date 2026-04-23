package com.derwin.prepforge.common.logging;

import java.util.Map;
import java.util.UUID;
import org.slf4j.MDC;

public final class LoggingContext {

    private LoggingContext() {
    }

    public static String getRequestId() {
        return MDC.get(LoggingConstants.REQUEST_ID_KEY);
    }

    public static String getCorrelationId() {
        String correlationId = MDC.get(LoggingConstants.CORRELATION_ID_KEY);
        return hasText(correlationId) ? correlationId : getRequestId();
    }

    public static String resolveCorrelationId() {
        String correlationId = getCorrelationId();
        return hasText(correlationId) ? correlationId : UUID.randomUUID().toString();
    }

    public static void putRequestId(String requestId) {
        if (hasText(requestId)) {
            MDC.put(LoggingConstants.REQUEST_ID_KEY, requestId);
        }
    }

    public static void putCorrelationId(String correlationId) {
        if (hasText(correlationId)) {
            MDC.put(LoggingConstants.CORRELATION_ID_KEY, correlationId);
        }
    }

    public static void putJobContext(String jobId, String jobType, String aggregateType, String aggregateId) {
        put(LoggingConstants.JOB_ID_KEY, jobId);
        put(LoggingConstants.JOB_TYPE_KEY, jobType);
        put(LoggingConstants.AGGREGATE_TYPE_KEY, aggregateType);
        put(LoggingConstants.AGGREGATE_ID_KEY, aggregateId);
    }

    public static Map<String, String> capture() {
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        return contextMap == null ? Map.of() : contextMap;
    }

    public static void restore(Map<String, String> context) {
        MDC.clear();
        if (context != null && !context.isEmpty()) {
            MDC.setContextMap(context);
        }
    }

    public static void clearJobContext() {
        MDC.remove(LoggingConstants.JOB_ID_KEY);
        MDC.remove(LoggingConstants.JOB_TYPE_KEY);
        MDC.remove(LoggingConstants.AGGREGATE_TYPE_KEY);
        MDC.remove(LoggingConstants.AGGREGATE_ID_KEY);
    }

    public static void clearRequestContext() {
        MDC.remove(LoggingConstants.REQUEST_ID_KEY);
        MDC.remove(LoggingConstants.CORRELATION_ID_KEY);
    }

    private static void put(String key, String value) {
        if (hasText(value)) {
            MDC.put(key, value);
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
