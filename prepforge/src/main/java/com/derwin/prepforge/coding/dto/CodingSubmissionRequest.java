package com.derwin.prepforge.coding.dto;

import jakarta.validation.constraints.NotBlank;
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
public class CodingSubmissionRequest {
    @NotBlank
    private String language;

    @NotBlank
    private String solutionCode;
}
