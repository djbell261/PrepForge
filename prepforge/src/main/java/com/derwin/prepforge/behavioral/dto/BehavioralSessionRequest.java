package com.derwin.prepforge.behavioral.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BehavioralSessionRequest {
    @NotNull
    private UUID questionId;

    private Boolean isTimed;

    @Positive
    private Integer timeLimitSeconds;
}
