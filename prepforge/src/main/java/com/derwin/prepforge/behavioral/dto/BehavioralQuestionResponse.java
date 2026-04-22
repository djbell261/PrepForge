package com.derwin.prepforge.behavioral.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BehavioralQuestionResponse {
    private final String questionId;
    private final String prompt;
    private final String category;
}
