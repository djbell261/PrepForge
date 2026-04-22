package com.derwin.prepforge.coding.dto;

import com.derwin.prepforge.coding.entity.CodingSessionStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodingSessionResponse {
    private UUID sessionId;
    private UUID questionId;
    private CodingSessionStatus status;
    private boolean timedMode;
    private Integer durationMinutes;
    private Instant expiresAt;
    private boolean expired;
    private Instant startedAt;
}
