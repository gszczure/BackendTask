package org.codibly.service;

import org.codibly.dto.response.DailyGenerationResponse;
import org.codibly.dto.response.GenerationResponse;
import org.codibly.dto.response.GenerationResponse.GenerationEntry;
import org.codibly.dto.response.OptimalChargingWindowResponse;
import org.codibly.exception.GenerationProviderConnectionException;
import org.codibly.exception.NoGenerationFoundExcepion;
import org.codibly.externalClient.CarbonIntensityClient;
import org.codibly.model.EnergySource;
import org.codibly.time.TimeProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class GenerationService {

    private final TimeProvider timeProvider;

    private final CarbonIntensityClient carbonIntensityClient;

    private static final DateTimeFormatter API_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm'Z'");

    private static final DateTimeFormatter DAY_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public GenerationService(TimeProvider timeProvider, CarbonIntensityClient carbonIntensityClient) {
        this.timeProvider = timeProvider;
        this.carbonIntensityClient = carbonIntensityClient;
    }

    /**
     * Fetches predicted electricity generation data for three days
     * and calculates the average share of each energy source
     * and the total clean energy percentage.
     *
     * @return a list of DTOs containing the daily averages
     */
    public List<DailyGenerationResponse> getThreeDaysAverage() {
        GenerationResponse response = fetchGenerationData(0,3);
        Map<String, List<GenerationEntry>> grouped = groupEntriesByDate(response);

        return calculateDailyAverages(grouped);
    }

    /**
     * Fetches generation data from the Carbon Intensity API for a given number of days.
     *
     * @param numberOfDays  number of days to fetch forecast data for
     * @return API response containing a list of generation entries
     * @throws NoGenerationFoundExcepion if no data is found in the API response
     * @throws GenerationProviderConnectionException if there is a connection issue with the API
     */
    private GenerationResponse fetchGenerationData(int startDayOffset, int numberOfDays) {
        ZonedDateTime startUtc = timeProvider.getStartOfDay().plusDays(startDayOffset);
        ZonedDateTime endUtc = timeProvider.getEndOfDay().plusDays(startDayOffset + numberOfDays);

        try {
            GenerationResponse raw = Optional.ofNullable(
                    carbonIntensityClient.getGenerationMix(
                            startUtc.format(API_FORMATTER),
                            endUtc.format(API_FORMATTER)
                    )
            ).orElseThrow(() -> new NoGenerationFoundExcepion("No generation data found for the requested period."));

            List<GenerationEntry> filtered = raw.data().stream()
                    .filter(e -> !e.from().isBefore(startUtc))
                    .filter(e -> !e.to().isAfter(endUtc))
                    .toList();

            return new GenerationResponse(filtered);

        } catch (RestClientException ex) {
            throw new GenerationProviderConnectionException("Failed to fetch data from CarbonIntensity API", ex);
        }
    }

    /**
     * Groups generation entries by date.
     *
     * @param response API response containing generation entries
     * @return a map where the key is the day (yyyy-MM-dd) and the value is the list of entries for that day
     */
    private Map<String, List<GenerationEntry>> groupEntriesByDate(GenerationResponse response) {
        return response.data().stream()
                .collect(Collectors.groupingBy(e -> e.from().format(DAY_FORMATTER)));
    }

    /**
     * Calculates daily averages for the grouped generation entries.
     * For each day, it computes:
     *  - the average share of each energy source (all sources),
     *  - the total clean energy percentage (sum of biomass, nuclear, hydro, wind, solar).
     *
     * @param grouped map of generation entries grouped by date
     * @return list of DTOs containing daily averages with clean energy percentage
     */

    private List<DailyGenerationResponse> calculateDailyAverages(Map<String, List<GenerationEntry>> grouped) {
        return grouped.entrySet().stream()
                .map(entry -> {
                    String day = entry.getKey();
                    Map<String, Double> avgMix = calculateAverageMix(entry.getValue());
                    double cleanPerc = calculateCleanEnergy(avgMix);
                    return new DailyGenerationResponse(day, avgMix, cleanPerc);
                })
                .sorted(Comparator.comparing(DailyGenerationResponse::date))
                .toList();
    }

    /**
     * Calculates the average share of each energy source for a list of generation entries.
     *
     * @param entries list of generation entries
     * @return a map where the key is the energy source and the value is the average percentage
     */
    private Map<String, Double> calculateAverageMix(List<GenerationEntry> entries) {
        return entries.stream()
                .flatMap(e -> e.generationmix().stream())
                .collect(Collectors.groupingBy(
                        GenerationEntry.FuelMix::fuel,
                        Collectors.averagingDouble(GenerationEntry.FuelMix::perc)
                ));
    }

    /**
     * Calculates the total clean energy percentage from the average mix of energy sources.
     * Clean energy sources are defined in the EnergySource enum.
     *
     * @param avgMix map of energy sources and their average percentages
     * @return total clean energy percentage as a double
     */
    private double calculateCleanEnergy(Map<String, Double> avgMix) {
        return avgMix.entrySet().stream()
                .filter(e -> Arrays.stream(EnergySource.values())
                        .anyMatch(es -> es.getFuelName().equals(e.getKey())))
                .mapToDouble(Map.Entry::getValue)
                .sum();
    }

    /**
     * Finds the optimal charging window for an electric vehicle based on the highest
     * average clean energy share for a given window length in hours.
     *
     * @param hours charging window length in full hours (1-6)
     * @return DTO containing start time, end time, and average clean energy percentage
     * @throws IllegalArgumentException if the window length is outside the range 1-6
     * @throws NoGenerationFoundExcepion if there is not enough data to calculate the window
     */
    public OptimalChargingWindowResponse findOptimalChargingWindow(int hours) {
        if (hours < 1 || hours > 6) {
            throw new IllegalArgumentException("Charging window length must be between 1 and 6 hours");
        }

        GenerationResponse response = fetchGenerationData(1, 2);
        List<GenerationEntry> entries = response.data();

        int windowSize = hours * 2;
        if (entries.size() < windowSize) {
            throw new NoGenerationFoundExcepion("Not enough data to calculate the optimal window");
        }

        double maxAverage = -1;
        int bestStartIndex = 0;

        for (int i = 0; i <= entries.size() - windowSize; i++) {
            List<GenerationEntry> windowEntries = entries.subList(i, i + windowSize);
            double avgClean = windowEntries.stream()
                    .mapToDouble(entry -> cleanEnergyStream(entry)
                            .mapToDouble(GenerationEntry.FuelMix::perc)
                            .sum())
                    .average()
                    .orElse(0.0);

            if (avgClean > maxAverage) {
                maxAverage = avgClean;
                bestStartIndex = i;
            }
        }

        GenerationEntry startEntry = entries.get(bestStartIndex);
        GenerationEntry endEntry = entries.get(bestStartIndex + windowSize - 1);

        return new OptimalChargingWindowResponse(
                startEntry.from(),
                endEntry.to(),
                maxAverage
        );
    }

    /**
     * Returns a stream of clean energy fuel mixes from a single generation entry.
     * Filters out only the fuels that are considered clean according to the EnergySource enum.
     *
     * @param entry a generation entry containing fuel mix information
     * @return a Stream of FuelMix objects representing clean energy sources
     */
    private Stream<GenerationEntry.FuelMix> cleanEnergyStream(GenerationEntry entry) {
        return entry.generationmix().stream()
                .filter(f -> Arrays.stream(EnergySource.values())
                        .anyMatch(es -> es.getFuelName().equals(f.fuel())));
    }
}
