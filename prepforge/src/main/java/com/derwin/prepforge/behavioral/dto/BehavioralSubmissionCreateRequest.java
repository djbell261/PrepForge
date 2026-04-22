package com.derwin.prepforge.behavioral.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BehavioralSubmissionCreateRequest {
    @NotNull
    private UUID sessionId;

    @NotBlank
    @Size(max = 20_000)
    private String responseText;
}
