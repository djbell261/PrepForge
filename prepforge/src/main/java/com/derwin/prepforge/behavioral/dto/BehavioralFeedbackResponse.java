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
public class BehavioralFeedbackResponse {
    private Integer score;
    private String summary;
    private List<String> improvements;
    private List<String> regressions;
    private List<String> strengths;
    private List<String> weaknesses;
    private List<String> recommendations;
}
