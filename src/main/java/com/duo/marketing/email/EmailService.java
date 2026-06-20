package com.duo.marketing.email;

import com.duo.marketing.config.AppProperties;
import com.duo.marketing.llm.ChannelDraft;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Emails the weekly digest of ready-to-paste marketing drafts.
 */
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final AppProperties props;

    public EmailService(JavaMailSender mailSender, AppProperties props) {
        this.mailSender = mailSender;
        this.props = props;
    }

    public void sendDigest(List<ChannelDraft> drafts) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(props.emailFrom());
        msg.setTo(props.emailTo());
        msg.setSubject("Marketing drafts — " + props.product().name() + " — " + LocalDate.now());
        msg.setText(buildBody(drafts));
        mailSender.send(msg);
    }

    private String buildBody(List<ChannelDraft> drafts) {
        StringBuilder b = new StringBuilder();
        b.append("Ready-to-paste marketing drafts for ").append(props.product().name()).append(".\n");
        b.append("Review, tweak, and post where each belongs.\n");

        for (ChannelDraft d : drafts) {
            b.append("\n========================================\n");
            b.append("CHANNEL: ").append(d.channelName()).append("\n");
            b.append("========================================\n");
            b.append(d.text()).append("\n");
        }
        return b.toString();
    }
}
