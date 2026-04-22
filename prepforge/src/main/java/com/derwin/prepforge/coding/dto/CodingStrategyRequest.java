package com.derwin.prepforge.coding.dto;

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
public class CodingStrategyRequest {
    private String clarificationQuestions;
    private String plannedApproach;
    private String expectedTimeComplexity;
    private String expectedSpaceComplexity;
}
