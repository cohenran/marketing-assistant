package com.duo.marketing.reddit;

/** A single Reddit post surfaced by the listener. */
public record RedditPost(
        String id,
        String subreddit,
        String title,
        String selftext,
        String url,
        int score
) {}
