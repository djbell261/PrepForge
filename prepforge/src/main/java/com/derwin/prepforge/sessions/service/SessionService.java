package com.derwin.prepforge.sessions.service;

import com.derwin.prepforge.common.enums.SessionStatus;
import com.derwin.prepforge.sessions.dto.SessionResponse;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class SessionService {

    public SessionResponse getSession(UUID sessionId) {
        return SessionResponse.builder()
                .sessionId(sessionId.toString())
                .status(SessionStatus.SCHEDULED)
                .sessionType("CODING")
                .build();
    }
}
