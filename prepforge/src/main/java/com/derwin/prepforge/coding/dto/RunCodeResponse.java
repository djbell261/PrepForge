package com.derwin.prepforge.coding.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RunCodeResponse {
    private boolean success;
    private Integer passedTests;
    private Integer totalTests;
    private String error;
    private CompileError compileError;
    private String friendlyMessage;
    private String rawOutput;
    private String runtimeError;
    private boolean timedOut;
    private List<RunTestResultResponse> testResults;
    private List<RunTestResultResponse> results;
}
