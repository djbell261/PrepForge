package com.derwin.prepforge.behavioral.dto;

import java.util.List;
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
public class BehavioralSessionDetailResponse {
    private BehavioralSessionResponse session;
    private BehavioralQuestionResponse question;
    private List<BehavioralSubmissionResponse> submissions;
}
