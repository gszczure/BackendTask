package org.codibly.controller;

import org.codibly.dto.response.DailyGenerationResponse;
import org.codibly.dto.response.OptimalChargingWindowResponse;
import org.codibly.service.GenerationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/")
public class GenerationController {

    private final GenerationService generationService;

    public GenerationController(GenerationService generationService) {
        this.generationService = generationService;
    }

    @GetMapping("generation/three-days")
    public ResponseEntity<List<DailyGenerationResponse>> getThreeDaysGeneration() {
        List<DailyGenerationResponse> result = generationService.getThreeDaysAverage();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(result);
    }

    @GetMapping("charge-window")
    public ResponseEntity<OptimalChargingWindowResponse> getOptimalChargingWindow(
            @RequestParam("hours") int hours) {
        OptimalChargingWindowResponse response = generationService.findOptimalChargingWindow(hours);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

}
