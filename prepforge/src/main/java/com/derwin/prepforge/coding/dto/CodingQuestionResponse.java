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
public class CodingQuestionResponse {
    private UUID questionId;
    private String title;
    private String difficulty;
    private String prompt;
}
