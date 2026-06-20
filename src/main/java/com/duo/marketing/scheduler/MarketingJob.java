package com.duo.marketing.scheduler;

import com.duo.marketing.channel.ChannelTarget;
import com.duo.marketing.config.AppProperties;
import com.duo.marketing.email.EmailService;
import com.duo.marketing.input.InputFiles;
import com.duo.marketing.llm.ChannelDraft;
import com.duo.marketing.llm.DraftGenerator;
import com.duo.marketing.persistence.GeneratedPost;
import com.duo.marketing.persistence.GeneratedPostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Orchestration. Runs on the cron and auto-picks which channels are DUE (per each
 * channel's cadenceDays + when it was last generated). Also supports a manual run
 * for one-shot channels via --run-now, and a no-email --dry-run (see StartupRunner).
 */
@Component
public class MarketingJob {

    private static final Logger log = LoggerFactory.getLogger(MarketingJob.class);

    private final AppProperties props;
    private final InputFiles inputs;
    private final DraftGenerator generator;
    private final EmailService email;
    private final GeneratedPostRepository repo;

    public MarketingJob(AppProperties props, InputFiles inputs, DraftGenerator generator,
                        EmailService email, GeneratedPostRepository repo) {
        this.props = props;
        this.inputs = inputs;
        this.generator = generator;
        this.email = email;
        this.repo = repo;
    }

    /** Scheduled run — generates only the channels that are due by cadence, then emails. */
    @Scheduled(cron = "${app.schedule-cron}")
    public void scheduledRun() {
        execute(Set.of());
    }

    /**
     * Generate due channels and email the digest.
     *
     * @param forced channel names to generate regardless of cadence. Empty = cadence-driven
     *               (scheduler). Non-empty = generate ONLY those channels (manual launch run).
     */
    public void execute(Set<String> forced) {
        log.info("=== Marketing run starting (forced={}) ===", forced);
        List<ChannelDraft> drafts = generateDue(forced, true);
        if (drafts.isEmpty()) {
            log.info("=== Nothing due this run — no email sent ===");
            return;
        }
        email.sendDigest(drafts);
        log.info("=== Run complete: {} channel drafts emailed ===", drafts.size());
    }

    /**
     * Generate without emailing or persisting — for the smoke test / dry run.
     * Always pass explicit channel names in {@code forced} so it produces output
     * regardless of cadence.
     */
    public List<ChannelDraft> generateOnly(Set<String> forced) {
        return generateDue(forced, false);
    }

    private List<ChannelDraft> generateDue(Set<String> forced, boolean persist) {
        String pains = inputs.painPoints();
        String voice = inputs.voice();
        String assets = inputs.assets();

        List<ChannelDraft> drafts = new ArrayList<>();
        for (ChannelTarget channel : props.channels()) {
            if (!isDue(channel, forced)) {
                continue;
            }
            List<String> past = repo.findTop3ByChannelOrderByGeneratedAtDesc(channel.name())
                    .stream().map(GeneratedPost::getText).toList();

            ChannelDraft draft = generator.generate(channel, pains, voice, assets, past);
            if (persist) {
                repo.save(new GeneratedPost(channel.name(), draft.text(), Instant.now()));
            }
            drafts.add(draft);
            log.info("Generated draft for channel '{}'{}", channel.name(), persist ? "" : " (dry run)");
        }
        return drafts;
    }

    private boolean isDue(ChannelTarget c, Set<String> forced) {
        if (!forced.isEmpty()) {
            // Forced run: only the named channels, cadence ignored.
            return forced.stream().anyMatch(f -> f.equalsIgnoreCase(c.name()));
        }
        // Scheduled run: skip manual-only channels, otherwise check cadence.
        if (c.cadenceDays() <= 0) {
            return false;
        }
        var last = repo.findTopByChannelOrderByGeneratedAtDesc(c.name());
        if (last.isEmpty()) {
            return true;  // never generated → due now
        }
        long daysSince = Duration.between(last.get().getGeneratedAt(), Instant.now()).toDays();
        return daysSince >= c.cadenceDays();
    }
}
