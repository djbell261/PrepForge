package com.derwin.prepforge.behavioral.controller;

import com.derwin.prepforge.behavioral.dto.BehavioralQuestionResponse;
import com.derwin.prepforge.behavioral.service.BehavioralService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/behavioral")
public class BehavioralController {

    private final BehavioralService behavioralService;

    @GetMapping("/questions/{questionId}")
    public ResponseEntity<BehavioralQuestionResponse> getQuestion(@PathVariable UUID questionId) {
        return ResponseEntity.ok(behavioralService.getQuestion(questionId));
    }
}
