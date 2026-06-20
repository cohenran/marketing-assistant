package com.duo.marketing.llm;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.ThinkingConfigAdaptive;
import com.duo.marketing.config.AppProperties;
import com.duo.marketing.promo.PromotionTarget;
import com.duo.marketing.reddit.RedditPost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Step 3 — draft generation via the Anthropic API (claude-opus-4-8).
 * The client reads ANTHROPIC_API_KEY from the environment (fromEnv()).
 *
 * Every draft is honest and transparent: it opens by disclosing that the author
 * built the app. No impersonating an organic commenter, no hidden marketing.
 */
@Service
public class DraftGenerator {

    private static final Logger log = LoggerFactory.getLogger(DraftGenerator.class);

    private static final String SYSTEM = """
            You write transparent, founder-voice promotional posts for a new dating app.

            The app's angle: it limits each user to 4 matches at a time, to push people
            toward fewer, deeper connections instead of endless swiping.

            Hard rules — follow all of them:
            1. OPEN WITH A DISCLOSURE. The first line must make it obvious the author built
               the app, e.g. "I built a dating app to fix X...". Never pose as a neutral user.
            2. Be honest. No fake stats, no invented testimonials, no manipulative urgency.
            3. Ground the copy in the REAL pain points provided, but do NOT quote or identify
               any individual Reddit user. Speak to the shared frustration, not a person.
            4. Tailor format and tone to the target platform:
               - REDDIT: conversational, humble, community-first. Include a title line.
               - FORUM (Indie Hackers / Hacker News): builder-to-builder, factual, what you
                 made and why. For Hacker News start the title with "Show HN:".
               - FACEBOOK_GROUP: warm, plain language, respect the group's purpose.
            5. Invite feedback, don't hard-sell. One clear, low-pressure call to action.
            6. Keep it tight. Provide a ready-to-paste post: a TITLE line (if the platform
               uses titles) then the BODY. No preamble, no meta commentary.
            """;

    private final AppProperties props;
    private final AnthropicClient client = AnthropicOkHttpClient.fromEnv();

    public DraftGenerator(AppProperties props) {
        this.props = props;
    }

    public List<PromoDraft> generate(List<RedditPost> painPoints, List<PromotionTarget> targets) {
        String painSummary = summarize(painPoints);
        List<PromoDraft> drafts = new ArrayList<>();
        for (PromotionTarget target : targets) {
            try {
                drafts.add(new PromoDraft(target.name(), target.url(), callModel(target, painSummary)));
            } catch (Exception e) {
                log.warn("Draft generation failed for {}: {}", target.name(), e.getMessage());
                drafts.add(new PromoDraft(target.name(), target.url(),
                        "[draft generation failed: " + e.getMessage() + "]"));
            }
        }
        return drafts;
    }

    private String callModel(PromotionTarget target, String painSummary) {
        String userMsg = """
                Target: %s
                Platform: %s
                Posting notes: %s

                Real pain points gathered from dating subreddits (research only — do not quote
                or identify anyone, just use the themes to ground the copy):
                %s

                Write one ready-to-paste promotional post for this target, following all rules.
                """.formatted(
                target.name(),
                target.platform(),
                target.notes() == null ? "(none)" : target.notes(),
                painSummary.isBlank() ? "(no posts gathered this run — write from the general theme of dating app fatigue)" : painSummary
        );

        MessageCreateParams params = MessageCreateParams.builder()
                .model(props.anthropic().model())          // "claude-opus-4-8"
                .maxTokens(4000)
                .system(SYSTEM)
                .thinking(ThinkingConfigAdaptive.builder().build())  // adaptive thinking
                .addUserMessage(userMsg)
                .build();

        Message message = client.messages().create(params);

        StringBuilder sb = new StringBuilder();
        message.content().forEach(block -> block.text().ifPresent(t -> sb.append(t.text())));
        return sb.toString().trim();
    }

    /** Compact, de-identified summary: title + score + link, capped to keep the prompt small. */
    private String summarize(List<RedditPost> posts) {
        return posts.stream()
                .limit(15)
                .map(p -> "- (%d pts, r/%s) %s".formatted(p.score(), p.subreddit(), p.title()))
                .collect(Collectors.joining("\n"));
    }
}
