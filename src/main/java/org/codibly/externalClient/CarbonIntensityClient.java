package org.codibly.externalClient;

import org.codibly.dto.response.GenerationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "carbonIntensityClient",
        url = "https://api.carbonintensity.org.uk"
)
public interface CarbonIntensityClient {

    @GetMapping("/generation/{from}/{to}")
    GenerationResponse getGenerationMix(
            @PathVariable(value = "from") String from,
            @PathVariable(value = "to") String to
    );
// TODO: Naprawic encoded w feign (encoded = true, nie dziala)
}
