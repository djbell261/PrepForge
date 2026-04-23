package com.derwin.prepforge.common.logging;

import com.derwin.prepforge.auth.entity.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
public class RequestCorrelationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String requestId = Optional.ofNullable(request.getHeader(LoggingConstants.REQUEST_ID_HEADER))
                .filter(value -> !value.isBlank())
                .orElseGet(() -> UUID.randomUUID().toString());
        long startedAt = System.currentTimeMillis();

        LoggingContext.putRequestId(requestId);
        LoggingContext.putCorrelationId(requestId);
        response.setHeader(LoggingConstants.REQUEST_ID_HEADER, requestId);

        log.info("http_request_start method={} path={} requestId={} remoteAddr={}",
                request.getMethod(),
                request.getRequestURI(),
                requestId,
                request.getRemoteAddr());

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - startedAt;
            log.info("http_request_end method={} path={} status={} durationMs={} requestId={} userId={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    durationMs,
                    requestId,
                    getCurrentUserId());
            LoggingContext.clearJobContext();
            LoggingContext.clearRequestContext();
        }
    }

    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            return "anonymous";
        }

        return user.getId().toString();
    }
}
