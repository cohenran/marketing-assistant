package com.duo.marketing.images;

import com.duo.marketing.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Searches Pexels for stock photos to suggest alongside each draft.
 * Disabled (returns empty) if no API key is configured, so the app still runs without it.
 * Pexels requires attribution — we keep the photographer + page URL for that.
 */
@Component
public class PexelsClient {

    private static final Logger log = LoggerFactory.getLogger(PexelsClient.class);

    private final AppProperties props;
    private final RestClient http = RestClient.builder().baseUrl("https://api.pexels.com/v1").build();

    public PexelsClient(AppProperties props) {
        this.props = props;
    }

    public boolean enabled() {
        AppProperties.Pexels p = props.pexels();
        return p != null && p.apiKey() != null && !p.apiKey().isBlank();
    }

    public List<PexelsImage> search(String query) {
        if (!enabled()) {
            return List.of();
        }
        try {
            JsonNode resp = http.get()
                    .uri(uri -> uri.path("/search")
                            .queryParam("query", query)
                            .queryParam("per_page", props.pexels().perPage())
                            .queryParam("orientation", "landscape")
                            .build())
                    .header("Authorization", props.pexels().apiKey())
                    .retrieve()
                    .body(JsonNode.class);

            List<PexelsImage> images = new ArrayList<>();
            if (resp != null) {
                for (JsonNode photo : resp.path("photos")) {
                    images.add(new PexelsImage(
                            photo.path("src").path("large").asText(),
                            photo.path("photographer").asText(),
                            photo.path("url").asText()
                    ));
                }
            }
            return images;
        } catch (Exception e) {
            log.warn("Pexels search failed for '{}': {}", query, e.getMessage());
            return List.of();
        }
    }
}
