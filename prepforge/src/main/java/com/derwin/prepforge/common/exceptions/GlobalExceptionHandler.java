package com.derwin.prepforge.common.exceptions;

import com.derwin.prepforge.common.ratelimit.AiRateLimitExceededException;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "timestamp", Instant.now().toString(),
                        "message", exception.getMessage()));
    }

    @ExceptionHandler(AiRateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleAiRateLimitExceeded(AiRateLimitExceededException exception) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.RETRY_AFTER, String.valueOf(exception.getRetryAfterSeconds()));

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .headers(headers)
                .body(Map.of(
                        "timestamp", Instant.now().toString(),
                        "message", "Too many AI requests. Please wait before trying again.",
                        "endpointCategory", exception.getEndpointCategory().getKey(),
                        "retryAfterSeconds", exception.getRetryAfterSeconds()));
    }
}
