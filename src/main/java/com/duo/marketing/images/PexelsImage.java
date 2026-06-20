package com.duo.marketing.images;

/** One Pexels photo result. pageUrl is the Pexels page (needed for attribution). */
public record PexelsImage(
        String url,
        String photographer,
        String pageUrl
) {}
