package org.codibly.dto.response;

import lombok.Builder;

import java.time.ZonedDateTime;

@Builder
public record OptimalChargingWindowResponse(
        ZonedDateTime start,
        ZonedDateTime end,
        double averageCleanEnergyPercentage
) {}
