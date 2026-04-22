package com.derwin.prepforge.behavioral.dto;

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
public class BehavioralQuestionResponse {
    private UUID id;
    private String questionText;
    private String category;
    private String difficulty;
}
