package org.codibly.dto.response;

import java.time.ZonedDateTime;

public record OptimalChargingWindowResponse(
        ZonedDateTime start,
        ZonedDateTime end,
        double averageCleanEnergyPercentage
) {}
