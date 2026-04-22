package com.derwin.prepforge.coding.dto;

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
public class CodingStrategyResponse {
    private UUID sessionId;
    private String clarificationQuestions;
    private String plannedApproach;
    private String expectedTimeComplexity;
    private String expectedSpaceComplexity;
}
