package com.derwin.prepforge.behavioral.dto;

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
public class BehavioralSubmissionResponse {
    private UUID submissionId;
    private String responseText;
    private BehavioralFeedbackResponse feedback;
    private Instant submittedAt;
}
