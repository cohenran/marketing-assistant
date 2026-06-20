package com.duo.marketing.config;

import com.duo.marketing.promo.PromotionTarget;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * All tunables live in application.yml under the "app" prefix.
 * Constructor binding (records) keeps config immutable.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String scheduleCron,
        String emailTo,
        String emailFrom,
        Reddit reddit,
        Listen listen,
        Anthropic anthropic,
        List<PromotionTarget> promotionTargets,
        List<String> facebookThemes
) {
    public record Reddit(String clientId, String clientSecret, String userAgent) {}

    public record Listen(
            List<String> subreddits,
            List<String> keywords,
            int minScore,
            int postLimit,
            String window
    ) {}

    public record Anthropic(String model) {}
}
