package com.derwin.prepforge.coding.dto;

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
public class CodingImprovementResponse {
    private String improvedCode;
    private List<String> explanation;
    private String timeComplexity;
    private String spaceComplexity;
}
