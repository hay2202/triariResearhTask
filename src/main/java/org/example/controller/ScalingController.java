package org.example.controller;

import org.example.model.ScalingRecommendation;
import org.example.service.ScalingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/scaling")
public class ScalingController {

    private final ScalingService scalingService;

    public ScalingController(ScalingService scalingService) {
        this.scalingService = scalingService;
    }

    @GetMapping("/recommendation")
    public ResponseEntity<ScalingRecommendation> getScalingRecommendation() {
        return ResponseEntity.ok(scalingService.getScalingRecommendation());
    }
}
