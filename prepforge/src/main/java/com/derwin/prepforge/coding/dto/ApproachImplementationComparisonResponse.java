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
public class ApproachImplementationComparisonResponse {
    private Integer alignmentScore;
    private String summary;
    private String matches;
    private String mismatches;
    private String improvementAreas;
    private String finalVerdict;
}
