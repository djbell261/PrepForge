package com.derwin.prepforge.summary.dto;

import java.time.Instant;
import java.util.List;
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
public class SessionSummaryResponse {
    private UUID id;
    private String sessionType;
    private UUID sessionId;
    private UUID userId;
    private String summary;
    private List<String> strengths;
    private List<String> weaknesses;
    private List<String> nextSteps;
    private Instant createdAt;
    private Instant updatedAt;
}
