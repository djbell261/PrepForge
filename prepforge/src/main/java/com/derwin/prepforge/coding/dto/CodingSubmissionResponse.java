package com.derwin.prepforge.coding.dto;

import com.derwin.prepforge.coding.entity.SubmissionStatus;
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
public class CodingSubmissionResponse {
    private UUID submissionId;
    private UUID sessionId;
    private String language;
    private SubmissionStatus status;
    private Instant submittedAt;
    private String aiFeedback;
}
