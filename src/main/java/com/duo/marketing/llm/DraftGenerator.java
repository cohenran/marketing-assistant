package com.duo.marketing.llm;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.duo.marketing.channel.ChannelTarget;
import com.duo.marketing.config.AppProperties;
import com.duo.marketing.images.PexelsClient;
import com.duo.marketing.images.PexelsImage;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.IntStream;

/**
 * Generates marketing copy for ONE channel via the Anthropic API (claude-opus-4-8),
 * then (if Pexels is configured) appends suggested stock photos.
 * The client reads ANTHROPIC_API_KEY from the environment (fromEnv()).
 *
 * Grounded in: product brief + your pain-point notes + your brand voice + your asset
 * inventory + the last few posts for this channel (so the new draft is materially
 * different). Honest founder voice, no fake claims. Output is a draft for YOU to edit
 * and post manually — the app never posts.
 */
@Service
public class DraftGenerator {

    private static final String SYSTEM = """
            You are a marketing copywriter for an app founder. You write honest,
            engaging, channel-native promotional copy.

            Rules:
            1. Founder voice — write as the person who built the app ("I built...").
            2. If a brand voice sample is provided, match its tone, rhythm, and vocabulary.
            3. Be honest. No fabricated stats, fake testimonials, or manipulative urgency.
            4. Ground the copy in the product facts and the audience pain points provided.
            5. Match the requested channel format and tone EXACTLY.
            6. Include the product link EXACTLY as given, verbatim, as a clear call to action.
            7. If previous posts for this channel are shown, make this one materially
               DIFFERENT — new angle, new hook, fresh phrasing. Never rehash them.
            8. After the post, add a line: "SUGGESTED VISUAL: <which listed asset to attach>".
               If no assets are listed, suggest what to capture.
            9. Then add a final line: "IMAGE SEARCH: <2-4 keyword stock-photo query>" — the
               search terms for a relevant stock photo for this post.
            10. Return only the draft, then the SUGGESTED VISUAL line, then the IMAGE SEARCH
                line. No preamble, no meta commentary.
            """;

    private final AppProperties props;
    private final PexelsClient pexels;
    private final AnthropicClient client = AnthropicOkHttpClient.fromEnv();

    public DraftGenerator(AppProperties props, PexelsClient pexels) {
        this.props = props;
        this.pexels = pexels;
    }

    public ChannelDraft generate(ChannelTarget channel,
                                 String painPoints,
                                 String voice,
                                 String assets,
                                 List<String> pastPosts) {
        AppProperties.Product p = props.product();
        String pastBlock = pastPosts.isEmpty()
                ? "(none yet — this is the first post for this channel)"
                : IntStream.range(0, pastPosts.size())
                    .mapToObj(i -> "--- previous #" + (i + 1) + " ---\n" + pastPosts.get(i))
                    .reduce((a, b) -> a + "\n\n" + b).orElse("");

        String userMsg = """
                Product:
                - Name: %s
                - Tagline: %s
                - What it is: %s
                - Pricing: %s
                - Link (include EXACTLY, verbatim, as the call to action): %s

                Audience pain points (the founder's own notes):
                %s

                Brand voice to mimic:
                %s

                Available visual assets (suggest which to attach):
                %s

                Channel: %s
                Required format: %s
                Channel notes: %s

                Previously generated posts for THIS channel (make the new one clearly different):
                %s

                Write one ready-to-paste %s draft following all rules.
                """.formatted(
                p.name(), p.tagline(), p.description(), p.pricing(), p.url(),
                blankOr(painPoints, "(none provided — work from the product facts and the anti-swiping angle)"),
                blankOr(voice, "(none provided — use a natural, honest founder voice)"),
                blankOr(assets, "(none listed — suggest a screenshot/video to capture)"),
                channel.name(), channel.format(), channel.notes() == null ? "(none)" : channel.notes(),
                pastBlock,
                channel.name()
        );

        MessageCreateParams params = MessageCreateParams.builder()
                .model(props.anthropic().model())   // "claude-opus-4-8"
                .maxTokens(4000)
                .system(SYSTEM)
                .addUserMessage(userMsg)
                .build();

        Message message = client.messages().create(params);

        StringBuilder sb = new StringBuilder();
        message.content().forEach(block -> block.text().ifPresent(t -> sb.append(t.text())));
        String text = sb.toString().trim();

        // Pull the model's IMAGE SEARCH query, fetch photos, then drop that meta line.
        String query = extractImageQuery(text);
        List<PexelsImage> images = pexels.search(query);
        String cleanText = stripImageSearchLine(text);

        return new ChannelDraft(channel.name(), cleanText, images, channel.htmlPaste());
    }

    private String stripImageSearchLine(String text) {
        return text.lines()
                .filter(line -> !line.trim().regionMatches(true, 0, "IMAGE SEARCH:", 0, "IMAGE SEARCH:".length()))
                .reduce((a, b) -> a + "\n" + b)
                .orElse("")
                .trim();
    }

    private String extractImageQuery(String text) {
        for (String line : text.split("\\R")) {
            String t = line.trim();
            if (t.regionMatches(true, 0, "IMAGE SEARCH:", 0, "IMAGE SEARCH:".length())) {
                String q = t.substring("IMAGE SEARCH:".length()).trim();
                if (!q.isBlank()) return q;
            }
        }
        return props.product().name() + " online dating connection";  // fallback query
    }

    private static String blankOr(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
