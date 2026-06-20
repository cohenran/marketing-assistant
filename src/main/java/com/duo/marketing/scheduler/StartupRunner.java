package com.duo.marketing.scheduler;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Manual one-shot trigger for launch channels (or any channel) on demand:
 *
 *   java -jar app.jar --run-now="Product Hunt,Show HN"
 *   ./run.sh --run-now="Product Hunt"
 *
 * Generates ONLY the named channels immediately (cadence ignored), emails them,
 * then the app keeps running on its normal schedule. Names are matched
 * case-insensitively against the channel names in application.yml.
 */
@Component
public class StartupRunner implements ApplicationRunner {

    private final MarketingJob job;

    public StartupRunner(MarketingJob job) {
        this.job = job;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!args.containsOption("run-now")) {
            return;
        }
        Set<String> forced = new HashSet<>();
        for (String value : args.getOptionValues("run-now")) {
            Arrays.stream(value.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(forced::add);
        }
        if (!forced.isEmpty()) {
            job.execute(forced);
        }
    }
}
