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
public class RunTestResultResponse {
    private String input;
    private String expectedOutput;
    private String actualOutput;
    private boolean passed;
}
