package com.derwin.prepforge.execution;

import com.derwin.prepforge.coding.dto.CompileError;
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
public class ExecutionResult {
    private boolean success;
    private Integer passedTests;
    private Integer totalTests;
    private String error;
    private CompileError compileError;
    private String friendlyMessage;
    private String rawOutput;
    private String runtimeError;
    private boolean timedOut;
    private List<TestCaseExecutionResult> testResults;
}
