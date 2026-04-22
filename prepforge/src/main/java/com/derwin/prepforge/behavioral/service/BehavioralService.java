package com.derwin.prepforge.behavioral.service;

import com.derwin.prepforge.behavioral.dto.BehavioralQuestionResponse;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class BehavioralService {

    public BehavioralQuestionResponse getQuestion(UUID questionId) {
        return BehavioralQuestionResponse.builder()
                .questionId(questionId.toString())
                .prompt("Tell me about a time you handled a conflict.")
                .category("LEADERSHIP")
                .build();
    }
}
