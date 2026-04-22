package com.derwin.prepforge.coding.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RunCodeRequest {
    private String language;
    private String solutionCode;
}