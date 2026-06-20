package com.duo.marketing.email;

import com.duo.marketing.config.AppProperties;
import com.duo.marketing.images.PexelsImage;
import com.duo.marketing.llm.ChannelDraft;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Sends the digest as an HTML email so Pexels images preview inline.
 *
 * Image handling per channel:
 *  - All channels: the suggested photos render as thumbnails so you can pick one.
 *  - htmlPaste channels (blog, newsletter): also get a ready copy-paste HTML snippet
 *    with the image embedded.
 *  - Other channels (LinkedIn, Twitter, Reddit, TikTok): plain post + image URLs to
 *    UPLOAD manually — those sites don't accept pasted HTML.
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
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
            helper.setFrom(props.emailFrom());
            helper.setTo(props.emailTo());
            helper.setSubject("Marketing drafts — " + props.product().name() + " — " + LocalDate.now());
            helper.setText(buildHtml(drafts), true);  // true = HTML
            mailSender.send(mime);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send digest email: " + e.getMessage(), e);
        }
    }

    private String buildHtml(List<ChannelDraft> drafts) {
        StringBuilder b = new StringBuilder();
        b.append("<div style=\"font-family:Arial,sans-serif;max-width:680px\">");
        b.append("<p>Ready-to-paste marketing drafts for <strong>")
                .append(esc(props.product().name())).append("</strong>. Edit, then post manually.</p>");

        for (ChannelDraft d : drafts) {
            b.append("<hr><h2 style=\"margin-bottom:4px\">").append(esc(d.channelName())).append("</h2>");

            // The post copy (monospace, preserves line breaks).
            b.append("<pre style=\"white-space:pre-wrap;background:#f6f6f6;padding:12px;border-radius:6px;font-family:Consolas,monospace\">")
                    .append(esc(d.text())).append("</pre>");

            // Image suggestions — always shown as thumbnails for preview.
            if (!d.images().isEmpty()) {
                b.append("<p><strong>Suggested images (Pexels):</strong></p>");
                for (PexelsImage img : d.images()) {
                    b.append("<div style=\"margin-bottom:8px\">")
                            .append("<img src=\"").append(esc(img.url()))
                            .append("\" width=\"320\" style=\"border-radius:6px;display:block\"/>")
                            .append("<small>photo by ").append(esc(img.photographer()))
                            .append(" — <a href=\"").append(esc(img.pageUrl())).append("\">Pexels</a>")
                            .append(" — <a href=\"").append(esc(img.url())).append("\">image link</a></small>")
                            .append("</div>");
                }

                if (d.htmlPaste()) {
                    // This site accepts HTML — give a copy-paste snippet with the first image embedded.
                    String snippet = htmlSnippet(d);
                    b.append("<p><strong>Copy-paste HTML (this site accepts it):</strong></p>");
                    b.append("<pre style=\"white-space:pre-wrap;background:#eef;padding:12px;border-radius:6px\">")
                            .append(esc(snippet)).append("</pre>");
                } else {
                    b.append("<p><em>This platform doesn't accept pasted HTML — paste the text above and ")
                            .append("<strong>upload one of the images manually</strong> (download via the image link). ")
                            .append("Credit the photographer.</em></p>");
                }
            }
        }
        b.append("</div>");
        return b.toString();
    }

    /** A minimal HTML version of the post with the first image embedded — for html-paste channels. */
    private String htmlSnippet(ChannelDraft d) {
        String body = "<p>" + esc(d.text()).replace("\n", "<br>") + "</p>";
        PexelsImage first = d.images().get(0);
        String img = "<img src=\"" + esc(first.url()) + "\" alt=\"\" style=\"max-width:100%\"/>"
                + "<br><small>Photo by " + esc(first.photographer()) + " on Pexels</small>";
        return body + "\n" + img;
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
