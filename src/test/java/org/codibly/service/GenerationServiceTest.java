package org.codibly.service;

import org.codibly.dto.response.DailyGenerationResponse;
import org.codibly.dto.response.GenerationResponse;
import org.codibly.dto.response.GenerationResponse.GenerationEntry;
import org.codibly.dto.response.GenerationResponse.GenerationEntry.FuelMix;
import org.codibly.dto.response.OptimalChargingWindowResponse;
import org.codibly.exception.GenerationProviderConnectionException;
import org.codibly.exception.NoGenerationFoundExcepion;
import org.codibly.externalClient.CarbonIntensityClient;
import org.codibly.model.EnergySource;
import org.codibly.time.TimeProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GenerationServiceTest {

    @Mock
    private TimeProvider timeProvider;

    @Mock
    private CarbonIntensityClient carbonIntensityClient;

    @InjectMocks
    private GenerationService generationService;

//    @BeforeEach
//    void setup() {
//        ReflectionTestUtils.setField(generationService, "restTemplate", restTemplateMock);
//    }

//    @Test
//    @DisplayName("Should find optimal charging window for 1-hour window")
//    void findOptimalChargingWindow_For_1HourWindow() {
//        // given
//        GenerationResponse mockResponse = mockGenerationResponse();
//
//        when(restTemplateMock.getForObject(anyString(), eq(GenerationResponse.class)))
//                .thenReturn(mockResponse);
//        when(timeProvider.getStartOfDay())
//                .thenReturn(ZonedDateTime.parse("2025-01-01T00:00Z"));
//        when(timeProvider.getEndOfDay())
//                .thenReturn(ZonedDateTime.parse("2025-01-04T00:00Z"));
//
//        // when
//        OptimalChargingWindowResponse result = generationService.findOptimalChargingWindow(1);
//
//        // then
//        GenerationEntry bestStart = mockResponse.data().get(2);
//        GenerationEntry bestEnd = mockResponse.data().get(3);
//
//        assertThat(result.start()).isEqualTo(bestStart.from());
//        assertThat(result.end()).isEqualTo(bestEnd.to());
//        assertThat(result.averageCleanEnergyPercentage()).isEqualTo(85);
//
//        verify(restTemplateMock).getForObject(anyString(), eq(GenerationResponse.class));
//    }
// TODO fix test

//    @Test
//    @DisplayName("Should find optimal 3-hour charging window")
//    void findOptimalChargingWindow__For3hour() {
//        // given
//        GenerationResponse mockResponse = mockGenerationResponse();
//
//        // zamiast restTemplate, mockujemy carbonIntensityClient
//        when(carbonIntensityClient.getGenerationMix(anyString(), anyString()))
//                .thenReturn(mockResponse);
//
//        when(timeProvider.getStartOfDay()).thenReturn(
//                ZonedDateTime.parse("2025-01-01T00:00Z"));
//        when(timeProvider.getEndOfDay()).thenReturn(
//                ZonedDateTime.parse("2025-01-04T00:00Z"));
//
//        // when
//        OptimalChargingWindowResponse result = generationService.findOptimalChargingWindow(3);
//
//        // then
//        assertThat(result.start()).isEqualTo(ZonedDateTime.parse("2025-01-02T00:00Z"));
//        assertThat(result.end()).isEqualTo(ZonedDateTime.parse("2025-01-03T01:30Z"));
//        assertThat(result.averageCleanEnergyPercentage()).isEqualTo(53.333333333333336);
//
//        verify(carbonIntensityClient).getGenerationMix(anyString(), anyString());
//        verify(timeProvider).getStartOfDay();
//        verify(timeProvider).getEndOfDay();
//    }

    @ParameterizedTest
    @ValueSource(ints = {0, 7, 999, -1})
    @DisplayName("Should throw IllegalArgumentException for invalid window lengths")
    void findOptimalChargingWindow_InvalidWindow_ShouldThrow_IllegalArgumentException(int invalidHoursWindow) {
        // when & then
        assertThatThrownBy(() -> generationService.findOptimalChargingWindow(invalidHoursWindow))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Charging window length must be between 1 and 6 hours");
    }

    @Test
    @DisplayName("Should calculate average clean energy for the next 3 days")
    void getThreeDaysAverage_ForNext3Days() {
        // given
        GenerationResponse resp = mockThreeDaysResponse();

        when(carbonIntensityClient.getGenerationMix(anyString(), anyString()))
                .thenReturn(resp);
        when(timeProvider.getStartOfDay()).thenReturn(
                ZonedDateTime.parse("2025-01-01T00:00Z"));
        when(timeProvider.getEndOfDay()).thenReturn(
                ZonedDateTime.parse("2025-01-04T00:00Z"));

        // when
        List<DailyGenerationResponse> result = generationService.getThreeDaysAverage();

        // then
        assertThat(result).hasSize(3);

        DailyGenerationResponse day1 = result.get(0);
        assertThat(day1.date()).isEqualTo("2025-01-01");
        assertThat(day1.cleanEnergyPerc()).isEqualTo(7);

        DailyGenerationResponse day2 = result.get(1);
        assertThat(day2.date()).isEqualTo("2025-01-02");
        assertThat(day2.cleanEnergyPerc()).isEqualTo(37.5);

        DailyGenerationResponse day3 = result.get(2);
        assertThat(day3.date()).isEqualTo("2025-01-03");
        assertThat(day3.cleanEnergyPerc()).isEqualTo(18);

        verify(timeProvider).getStartOfDay();
        verify(timeProvider).getEndOfDay();
    }

    @Test
    @DisplayName("Should throw NoGenerationFoundExcepion when API returns no data")
    void fetchGenerationData_NoData_ShouldThrow_NoGenerationFoundExcepion() {
        // given
        when(carbonIntensityClient.getGenerationMix(anyString(), anyString()))
                .thenReturn(null);
        when(timeProvider.getStartOfDay())
                .thenReturn(ZonedDateTime.parse("2025-01-01T00:00Z"));
        when(timeProvider.getEndOfDay())
                .thenReturn(ZonedDateTime.parse("2025-01-02T00:00Z"));

        // when & then
        assertThatThrownBy(() -> generationService.getThreeDaysAverage())
                .isInstanceOf(NoGenerationFoundExcepion.class)
                .hasMessageContaining("No generation data found for the requested period");
    }

    @Test
    @DisplayName("Should throw GenerationProviderConnectionException when API fails")
    void fetchGenerationData_ApiFails_ShouldThrow_GenerationProviderConnectionException() {
        // given
        when(carbonIntensityClient.getGenerationMix(anyString(), anyString()))
                .thenThrow(new RestClientException("API down"));
        when(timeProvider.getStartOfDay())
                .thenReturn(ZonedDateTime.parse("2025-01-01T00:00Z"));
        when(timeProvider.getEndOfDay())
                .thenReturn(ZonedDateTime.parse("2025-01-02T00:00Z"));

        // when & then
        assertThatThrownBy(() -> generationService.getThreeDaysAverage())
                .isInstanceOf(GenerationProviderConnectionException.class)
                .hasMessageContaining("Failed to fetch data from CarbonIntensity API");
    }

    private GenerationResponse mockGenerationResponse() {

        GenerationEntry today1 = createEntry("2025-01-01T23:00Z", "2025-01-01T23:30Z",
                0.0, 999999.0, 0.0, 99999.0, 0.0);
        GenerationEntry today2 = createEntry("2025-01-01T23:30Z", "2025-01-02T00:00Z",
                0.0, 8099999.0, 0.0, 8099999.0, 0.0);
        GenerationEntry d2e1 = createEntry("2025-01-02T00:00Z", "2025-01-02T00:30Z",
                0.0, 10.0, 0.0, 20.0, 0.0);
        GenerationEntry d2e2 = createEntry("2025-01-02T00:30Z", "2025-01-02T01:00Z",
                0.0, 15.0, 0.0, 25.0, 0.0);
        GenerationEntry d2e3 = createEntry("2025-01-02T01:00Z", "2025-01-02T01:30Z",
                0.0, 20.0, 0.0, 30.0, 0.0);
        GenerationEntry d3e1 = createEntry("2025-01-03T00:00Z", "2025-01-03T00:30Z",
                0.0, 250., 0.0, 35.0, 0.0);
        GenerationEntry d3e2 = createEntry("2025-01-03T00:30Z", "2025-01-03T01:00Z",
                0.0, 30.0, 0.0, 40.0, 0.0);
        GenerationEntry d3e3 = createEntry("2025-01-03T01:00Z", "2025-01-03T01:30Z",
                0.0, 30.0, 0.0, 40.0, 0.0);
        GenerationEntry d4e1 = createEntry("2025-01-04T00:00Z", "2025-01-04T00:30Z",
                0.0, 30.0, 0.0, 40.0, 0.0);
        GenerationEntry farFuture = createEntry("3000-01-01T00:00Z", "3000-01-01T01:00Z",
                1111,0.0,0.0,0.0,0.0);


        return new GenerationResponse(List.of(today1, today2, d2e1, d2e2, d2e3, d3e1, d3e2, d3e3, d4e1, farFuture));
    }

    private GenerationResponse mockThreeDaysResponse() {
        GenerationEntry d1e1 = createEntry("2025-01-01T00:00Z", "2025-01-01T00:30Z",
                0.0, 5.0, 0.0, 2.0, 0.0);
        GenerationEntry d1e2 = createEntry("2025-01-01T00:00Z", "2025-01-01T01:00Z",
                0.0, 5.0, 0.0, 2.0, 0.0);
        GenerationEntry d2e1 = createEntry("2025-01-02T00:00Z", "2025-01-02T00:30Z",
                0.0, 10.0, 0.0, 50.0, 0.0);
        GenerationEntry d2e2 = createEntry("2025-01-02T00:30Z", "2025-01-02T01:00Z",
                0.0, 2.0, 0.0, 13.0, 0.0);
        GenerationEntry d3e1 = createEntry("2025-01-03T00:00Z", "2025-01-03T00:30Z",
                0.0, 5.0, 0.0, 13.0, 0.0);
        GenerationEntry d3e2 = createEntry("2025-01-03T00:30Z", "2025-01-03T10:00Z",
                0.0, 5.0, 0.0, 13.0, 0.0);

        return new GenerationResponse(List.of(d1e1, d1e2, d2e1, d2e2, d3e1, d3e2));
    }

    private GenerationEntry createEntry(String from, String to, double biomass, double nuclear, double hydro, double wind, double solar) {
        return entry(from, to, Map.of(
                EnergySource.BIOMASS, biomass,
                EnergySource.NUCLEAR, nuclear,
                EnergySource.HYDRO, hydro,
                EnergySource.WIND, wind,
                EnergySource.SOLAR, solar
        ));
    }

    private GenerationEntry entry(String from, String to, Map<EnergySource, Double> energyShare) {
        return new GenerationEntry(
                ZonedDateTime.parse(from),
                ZonedDateTime.parse(to),
                energyShare.entrySet().stream()
                        .map(e -> new FuelMix(e.getKey().getFuelName(), e.getValue()))
                        .toList()
        );
    }

}