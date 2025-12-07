package org.codibly.controller;

import org.codibly.dto.response.DailyGenerationResponse;
import org.codibly.service.GenerationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class GenerationController {

    private final GenerationService generationService;

    public GenerationController(GenerationService generationService) {
        this.generationService = generationService;
    }

    @GetMapping("/api/generation/three-days")
    public ResponseEntity<List<DailyGenerationResponse>> getThreeDaysGeneration() {
        List<DailyGenerationResponse> result = generationService.getThreeDaysAverage();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(result);
    }

}
