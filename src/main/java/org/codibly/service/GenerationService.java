package org.codibly.service;

import org.codibly.dto.response.DailyGenerationResponse;
import org.codibly.dto.response.GenerationResponse;
import org.codibly.dto.response.GenerationResponse.GenerationEntry;
import org.codibly.model.EnergySource;
import org.codibly.time.TimeProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GenerationService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final TimeProvider timeProvider;

    private static final DateTimeFormatter API_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm'Z'");

    private static final DateTimeFormatter DAY_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public GenerationService(TimeProvider timeProvider) {
        this.timeProvider = timeProvider;
    }

    public List<DailyGenerationResponse> getThreeDaysAverage() {
        GenerationResponse response = fetchGenerationData();
        Map<String, List<GenerationEntry>> grouped = groupEntriesByDate(response);

        return calculateDailyAverages(grouped);
    }

    //TODO przeniesc to do CarbonIntensityClient
    private GenerationResponse fetchGenerationData() {
        OffsetDateTime startUtc = timeProvider.get().toOffsetDateTime();
        OffsetDateTime endUtc = startUtc.plusDays(3);

        String url = "https://api.carbonintensity.org.uk/generation/"
                + startUtc.format(API_FORMATTER) + "/" + endUtc.format(API_FORMATTER);
        GenerationResponse response = restTemplate.getForObject(url, GenerationResponse.class);

        return response != null ? response : new GenerationResponse(Collections.emptyList());
    }

    private Map<String, List<GenerationEntry>> groupEntriesByDate(GenerationResponse response) {
        return response.data().stream()
                .collect(Collectors.groupingBy(e -> e.from().format(DAY_FORMATTER)));
    }

    private List<DailyGenerationResponse> calculateDailyAverages(Map<String, List<GenerationEntry>> grouped) {
        return grouped.entrySet().stream()
                //TODO: zastanowić sie nam mappowaniem w package mapper
                .map(entry -> {
                    String day = entry.getKey();
                    Map<String, Double> avgMix = calculateAverageMix(entry.getValue());
                    double cleanPerc = avgMix.values().stream().mapToDouble(Double::doubleValue).sum();
                    return new DailyGenerationResponse(day, avgMix, cleanPerc);
                })
                .sorted(Comparator.comparing(DailyGenerationResponse::date))
                .toList();
    }

    private Map<String, Double> calculateAverageMix(List<GenerationEntry> entries) {
        int count = entries.size();

        return entries.stream()
                .flatMap(entry -> entry.generationmix().stream())
                .filter(f -> Arrays.stream(EnergySource.values())
                        .anyMatch(es -> es.getFuelName().equals(f.fuel())))
                .collect(Collectors.groupingBy(
                        GenerationEntry.FuelMix::fuel,
                        Collectors.summingDouble(GenerationEntry.FuelMix::perc)
                ))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue() / count
                ));
    }
}
