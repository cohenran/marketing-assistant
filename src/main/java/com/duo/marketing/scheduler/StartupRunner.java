package com.duo.marketing.scheduler;

import com.duo.marketing.config.AppProperties;
import com.duo.marketing.llm.ChannelDraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Command-line entry points, evaluated once at startup:
 *
 *   --dry-run [--run-now="Channel"]   Generate to a local file, NO email. Only needs
 *                                     ANTHROPIC_API_KEY. Writes smoke-test-output.txt,
 *                                     then the app exits. Defaults to the first channel
 *                                     if no --run-now given. Nothing is saved to the DB.
 *
 *   --run-now="A,B"                   Generate those channels now (cadence ignored) and
 *                                     email the digest. App keeps running on its schedule.
 *
 * With no options, the app just runs on its normal schedule.
 */
@Component
public class StartupRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupRunner.class);
    private static final Path OUTPUT = Path.of("smoke-test-output.txt");

    private final MarketingJob job;
    private final AppProperties props;
    private final ConfigurableApplicationContext ctx;

    public StartupRunner(MarketingJob job, AppProperties props, ConfigurableApplicationContext ctx) {
        this.job = job;
        this.props = props;
        this.ctx = ctx;
    }

    @Override
    public void run(ApplicationArguments args) {
        Set<String> forced = parseRunNow(args);

        if (args.containsOption("dry-run")) {
            dryRun(forced);
            // smoke test is one-and-done — shut down cleanly
            System.exit(SpringApplication.exit(ctx, () -> 0));
        } else if (!forced.isEmpty()) {
            job.execute(forced);
        }
    }

    private void dryRun(Set<String> forced) {
        // Default to the first channel so there's always something to show.
        if (forced.isEmpty() && !props.channels().isEmpty()) {
            forced = Set.of(props.channels().get(0).name());
        }
        log.info("DRY RUN — generating {} (no email, not saved to DB)", forced);

        List<ChannelDraft> drafts = job.generateOnly(forced);
        String body = render(drafts);

        try {
            Files.writeString(OUTPUT, body);
            log.info("DRY RUN — wrote {} draft(s) to {}", drafts.size(), OUTPUT.toAbsolutePath());
        } catch (IOException e) {
            log.error("DRY RUN — could not write {}: {}", OUTPUT, e.getMessage());
        }
        // Also echo to console so you see it immediately.
        System.out.println("\n" + body);
    }

    private String render(List<ChannelDraft> drafts) {
        if (drafts.isEmpty()) {
            return "No drafts generated. Check the channel name passed to --run-now.";
        }
        StringBuilder b = new StringBuilder();
        b.append("SMOKE TEST — ").append(props.product().name()).append(" — drafts (not emailed, not saved)\n");
        for (ChannelDraft d : drafts) {
            b.append("\n========================================\n");
            b.append("CHANNEL: ").append(d.channelName()).append("\n");
            b.append("========================================\n");
            b.append(d.text()).append("\n");
        }
        return b.toString();
    }

    private Set<String> parseRunNow(ApplicationArguments args) {
        Set<String> forced = new HashSet<>();
        if (args.containsOption("run-now")) {
            for (String value : args.getOptionValues("run-now")) {
                Arrays.stream(value.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .forEach(forced::add);
            }
        }
        return forced;
    }
}
