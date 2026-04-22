package com.derwin.prepforge.behavioral.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BehavioralSessionRequest {
    @NotNull
    private UUID questionId;
}
