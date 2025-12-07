package org.codibly.dto.response;

import java.util.Map;

public record DailyGenerationResponse(
        String date,
        Map<String, Double> energyMix,
        double cleanEnergyPerc
) {}
