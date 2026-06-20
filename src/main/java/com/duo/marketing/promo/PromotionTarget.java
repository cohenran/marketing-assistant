package com.duo.marketing.promo;

/**
 * A place where self-promotion is explicitly allowed.
 * platform: REDDIT | FORUM | FACEBOOK_GROUP — used to tailor the draft format.
 * notes: posting rules / disclosure hints (may be null).
 */
public record PromotionTarget(
        String name,
        String url,
        String platform,
        String notes
) {}
