package com.derwin.prepforge.execution;

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
public class ExecutionTestCase {
    private String displayInput;
    private String programInput;
    private String expectedOutput;
}
