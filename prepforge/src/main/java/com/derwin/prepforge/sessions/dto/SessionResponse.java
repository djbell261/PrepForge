package com.derwin.prepforge.sessions.dto;

import com.derwin.prepforge.common.enums.SessionStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SessionResponse {
    private final String sessionId;
    private final SessionStatus status;
    private final String sessionType;
}
