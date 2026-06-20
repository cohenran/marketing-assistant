package com.duo.marketing.email;

import com.duo.marketing.config.AppProperties;
import com.duo.marketing.llm.PromoDraft;
import com.duo.marketing.reddit.RedditPost;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Step 4 — emails the digest: gathered pain points, the approved target URLs,
 * and the exact copy-paste drafts.
 */
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final AppProperties props;

    public EmailService(JavaMailSender mailSender, AppProperties props) {
        this.mailSender = mailSender;
        this.props = props;
    }

    public void sendDigest(List<RedditPost> painPoints, List<PromoDraft> drafts) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(props.emailFrom());
        msg.setTo(props.emailTo());
        msg.setSubject("Transparent Growth digest — " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        msg.setText(buildBody(painPoints, drafts));
        mailSender.send(msg);
    }

    private String buildBody(List<RedditPost> painPoints, List<PromoDraft> drafts) {
        StringBuilder b = new StringBuilder();

        b.append("=== PAIN POINTS (market research — do not reply to these) ===\n\n");
        if (painPoints.isEmpty()) {
            b.append("No matching posts this run.\n");
        } else {
            for (RedditPost p : painPoints) {
                b.append("• [").append(p.score()).append(" pts] r/").append(p.subreddit())
                        .append(" — ").append(p.title()).append("\n  ").append(p.url()).append("\n");
            }
        }

        b.append("\n\n=== APPROVED PROMO DRAFTS (copy-paste where self-promo is allowed) ===\n");
        for (PromoDraft d : drafts) {
            b.append("\n----------------------------------------\n");
            b.append("TARGET : ").append(d.targetName()).append("\n");
            b.append("URL    : ").append(d.url()).append("\n");
            b.append("----------------------------------------\n");
            b.append(d.text()).append("\n");
        }

        b.append("\n\nReminder: only post where the platform's rules explicitly allow self-promotion.\n");
        return b.toString();
    }
}
