package com.duo.marketing.scheduler;

import com.duo.marketing.email.EmailService;
import com.duo.marketing.listener.PainPointService;
import com.duo.marketing.llm.DraftGenerator;
import com.duo.marketing.llm.PromoDraft;
import com.duo.marketing.promo.PromotionTarget;
import com.duo.marketing.promo.PromotionTargetService;
import com.duo.marketing.reddit.RedditPost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Orchestrates the hourly run: listen -> source targets -> draft -> email.
 */
@Component
public class MarketingJob {

    private static final Logger log = LoggerFactory.getLogger(MarketingJob.class);

    private final PainPointService painPoints;
    private final PromotionTargetService targets;
    private final DraftGenerator drafts;
    private final EmailService email;

    public MarketingJob(PainPointService painPoints,
                        PromotionTargetService targets,
                        DraftGenerator drafts,
                        EmailService email) {
        this.painPoints = painPoints;
        this.targets = targets;
        this.drafts = drafts;
        this.email = email;
    }

    @Scheduled(cron = "${app.schedule-cron}")
    public void run() {
        log.info("=== Transparent Growth run starting ===");
        try {
            List<RedditPost> pains = painPoints.gather();
            List<PromotionTarget> promoTargets = targets.targets();
            List<PromoDraft> generated = drafts.generate(pains, promoTargets);
            email.sendDigest(pains, generated);
            log.info("=== Run complete: {} pain points, {} drafts emailed ===",
                    pains.size(), generated.size());
        } catch (Exception e) {
            log.error("Run failed: {}", e.getMessage(), e);
        }
    }
}
