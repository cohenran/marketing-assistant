package com.duo.marketing.reddit;

import com.duo.marketing.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Read-only Reddit access via application-only OAuth (grant_type=client_credentials).
 * Confidential "script" apps (client id + secret) can use this flow for reading
 * public listings — no Reddit username/password stored.
 */
@Component
public class RedditClient {

    private final AppProperties props;
    private final RestClient auth = RestClient.builder().baseUrl("https://www.reddit.com").build();
    private final RestClient api = RestClient.builder().baseUrl("https://oauth.reddit.com").build();

    private String token;
    private Instant expiry = Instant.EPOCH;

    public RedditClient(AppProperties props) {
        this.props = props;
    }

    private synchronized String token() {
        if (Instant.now().isBefore(expiry) && token != null) {
            return token;
        }
        AppProperties.Reddit r = props.reddit();
        String basic = Base64.getEncoder().encodeToString(
                (r.clientId() + ":" + r.clientSecret()).getBytes(StandardCharsets.UTF_8));

        JsonNode resp = auth.post()
                .uri("/api/v1/access_token")
                .header("Authorization", "Basic " + basic)
                .header("User-Agent", r.userAgent())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("grant_type=client_credentials")
                .retrieve()
                .body(JsonNode.class);

        token = resp.path("access_token").asText();
        long ttl = resp.path("expires_in").asLong(3600);
        expiry = Instant.now().plusSeconds(ttl - 60);  // refresh a minute early
        return token;
    }

    /** Search one subreddit, restricted to that sub, newest first. */
    public List<RedditPost> search(String subreddit, String query, int limit, String window) {
        JsonNode resp = api.get()
                .uri(uri -> uri.path("/r/{sub}/search")
                        .queryParam("q", query)
                        .queryParam("restrict_sr", "1")
                        .queryParam("sort", "new")
                        .queryParam("t", window)
                        .queryParam("limit", limit)
                        .build(subreddit))
                .header("Authorization", "bearer " + token())
                .header("User-Agent", props.reddit().userAgent())
                .retrieve()
                .body(JsonNode.class);

        List<RedditPost> posts = new ArrayList<>();
        for (JsonNode child : resp.path("data").path("children")) {
            JsonNode d = child.path("data");
            posts.add(new RedditPost(
                    d.path("id").asText(),
                    d.path("subreddit").asText(),
                    d.path("title").asText(),
                    d.path("selftext").asText(""),
                    "https://www.reddit.com" + d.path("permalink").asText(),
                    d.path("score").asInt()
            ));
        }
        return posts;
    }
}
