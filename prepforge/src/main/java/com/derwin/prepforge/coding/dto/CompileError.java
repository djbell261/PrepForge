package com.derwin.prepforge.coding.dto;

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
public class CompileError {
    private Integer line;
    private Integer column;
    private String message;
    private String codeLine;
}
