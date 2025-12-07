package org.codibly.dto.response;

import java.time.OffsetDateTime;

public record OptimalChargingWindowResponse(
        OffsetDateTime start,
        OffsetDateTime end,
        double averageCleanEnergyPercentage
) {}
