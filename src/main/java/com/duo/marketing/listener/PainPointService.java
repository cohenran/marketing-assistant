package com.duo.marketing.listener;

import com.duo.marketing.config.AppProperties;
import com.duo.marketing.reddit.RedditClient;
import com.duo.marketing.reddit.RedditPost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Step 1 — pain-point listener. Pure market research, read only.
 * Searches each listen-subreddit for the keyword set, keeps posts that clear the
 * score gate AND actually contain a keyword, deduped by post id.
 */
@Service
public class PainPointService {

    private static final Logger log = LoggerFactory.getLogger(PainPointService.class);

    private final RedditClient reddit;
    private final AppProperties props;

    public PainPointService(RedditClient reddit, AppProperties props) {
        this.reddit = reddit;
        this.props = props;
    }

    public List<RedditPost> gather() {
        AppProperties.Listen l = props.listen();
        // Reddit search q caps at ~512 chars; keep the keyword list modest.
        String query = String.join(" OR ", l.keywords().stream().map(k -> "\"" + k + "\"").toList());

        Map<String, RedditPost> deduped = new LinkedHashMap<>();
        for (String sub : l.subreddits()) {
            try {
                for (RedditPost p : reddit.search(sub, query, l.postLimit(), l.window())) {
                    if (p.score() >= l.minScore() && matchesKeyword(p, l.keywords())) {
                        deduped.putIfAbsent(p.id(), p);
                    }
                }
            } catch (Exception e) {
                log.warn("Reddit search failed for r/{}: {}", sub, e.getMessage());
            }
        }
        log.info("Pain-point listener gathered {} relevant posts", deduped.size());
        return new ArrayList<>(deduped.values());
    }

    private boolean matchesKeyword(RedditPost p, List<String> keywords) {
        String hay = (p.title() + " " + p.selftext()).toLowerCase();
        return keywords.stream().anyMatch(k -> hay.contains(k.toLowerCase()));
    }
}
