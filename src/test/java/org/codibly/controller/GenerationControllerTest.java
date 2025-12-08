package org.codibly.controller;

import org.codibly.dto.response.DailyGenerationResponse;
import org.codibly.dto.response.OptimalChargingWindowResponse;
import org.codibly.service.GenerationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GenerationController.class)
class GenerationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GenerationService generationService;

    @Test
    @DisplayName("Should return a list of three days with correct generation values")
    void getThreeDaysGeneration_shouldReturnOkAndList() throws Exception {
        // given
        when(generationService.getThreeDaysAverage()).thenReturn(createDailyGenerationResponses());

        // when & then
        mockMvc.perform(get("/api/v1/generation/three-days")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].date").value("2025-01-01"))
                .andExpect(jsonPath("$[0].cleanEnergyPerc").value(7))
                .andExpect(jsonPath("$[0].energyMix.solar").value(2.0))
                .andExpect(jsonPath("$[0].energyMix.wind").value(5.0))
                .andExpect(jsonPath("$[1].date").value("2025-01-02"))
                .andExpect(jsonPath("$[1].cleanEnergyPerc").value(37.5))
                .andExpect(jsonPath("$[1].energyMix.solar").value(27.5))
                .andExpect(jsonPath("$[1].energyMix.wind").value(10.0))
                .andExpect(jsonPath("$[2].date").value("2025-01-03"))
                .andExpect(jsonPath("$[2].cleanEnergyPerc").value(18))
                .andExpect(jsonPath("$[2].energyMix.solar").value(9.0))
                .andExpect(jsonPath("$[2].energyMix.wind").value(9.0));

    }

    @Test
    @DisplayName("Should return the optimal charging window with correct values")
    void getOptimalChargingWindow_shouldReturnOkAndWindow() throws Exception {
        // given
        when(generationService.findOptimalChargingWindow(anyInt())).thenReturn(createOptimalChargingWindowResponse());

        // when & then
        mockMvc.perform(get("/api/v1/charge-window")
                        .param("hours", "3")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.start").value("2025-12-10T03:00:00Z"))
                .andExpect(jsonPath("$.end").value("2025-12-10T06:00:00Z"))
                .andExpect(jsonPath("$.averageCleanEnergyPercentage").value(15.0));
    }

    private OptimalChargingWindowResponse createOptimalChargingWindowResponse() {
        return OptimalChargingWindowResponse.builder()
                .start(ZonedDateTime.parse("2025-12-10T03:00:00Z"))
                .end(ZonedDateTime.parse("2025-12-10T06:00:00Z"))
                .averageCleanEnergyPercentage(15)
                .build();
    }

    private List<DailyGenerationResponse> createDailyGenerationResponses() {
        DailyGenerationResponse d1 = createDaily("2025-01-01", 7.0, Map.of("solar", 2.0, "wind", 5.0));
        DailyGenerationResponse d2 = createDaily("2025-01-02", 37.5, Map.of("solar", 27.5, "wind", 10.0));
        DailyGenerationResponse d3 = createDaily("2025-01-03", 18.0, Map.of("solar", 9.0, "wind", 9.0));
        return List.of(d1, d2, d3);
    }

    private DailyGenerationResponse createDaily(String date, double cleanEnergyPerc, Map<String, Double> energyMix) {
        return new DailyGenerationResponse(date, energyMix, cleanEnergyPerc);
    }
}
