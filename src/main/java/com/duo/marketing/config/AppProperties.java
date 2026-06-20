package com.duo.marketing.config;

import com.duo.marketing.channel.ChannelTarget;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * All tunables live in application.yml under the "app" prefix.
 * Inputs are your own files: pain points, brand voice, and available visual assets.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String scheduleCron,
        String emailTo,
        String emailFrom,
        String painPointsFile,
        String voiceFile,
        String assetsFile,
        Product product,
        Anthropic anthropic,
        Pexels pexels,
        List<ChannelTarget> channels
) {
    /** Facts about your app — the source material every draft is built from. */
    public record Product(
            String name,
            String tagline,
            String url,
            String description,
            String pricing
    ) {}

    public record Anthropic(String model) {}

    /** Optional stock-photo lookup. Disabled when apiKey is blank. */
    public record Pexels(String apiKey, int perPage) {}
}
