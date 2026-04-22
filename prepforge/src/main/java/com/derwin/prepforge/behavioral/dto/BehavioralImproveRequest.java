package com.derwin.prepforge.behavioral.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BehavioralImproveRequest {
    @NotBlank
    @Size(max = 20_000)
    private String responseText;

    @Valid
    private FeedbackInput feedback;

    @Getter
    @Setter
    public static class FeedbackInput {
        private List<String> strengths;
        private List<String> weaknesses;
    }
}
