package com.derwin.prepforge.behavioral.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BehavioralSubmissionRequest {
    @NotBlank
    @Size(max = 20_000)
    private String responseText;
}
