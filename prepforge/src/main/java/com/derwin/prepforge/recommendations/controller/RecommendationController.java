package com.derwin.prepforge.recommendations.controller;

import com.derwin.prepforge.recommendations.dto.RecommendationResponse;
import com.derwin.prepforge.recommendations.service.RecommendationService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/{recommendationId}")
    public ResponseEntity<RecommendationResponse> getRecommendation(@PathVariable UUID recommendationId) {
        return ResponseEntity.ok(recommendationService.getRecommendation(recommendationId));
    }
}
