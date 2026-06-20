package com.duo.marketing.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

import java.time.Instant;

/**
 * A record of a draft the app generated, per channel. Used to (a) decide when a
 * channel is due again and (b) feed past posts back into the prompt so each new
 * draft is materially different (avoids repetitive / spammy copy).
 *
 * Note: this records what was GENERATED. Editing and posting stay manual — the app
 * never posts anything.
 */
@Entity
public class GeneratedPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String channel;

    @Lob
    @Column(nullable = false)
    private String text;

    @Column(nullable = false)
    private Instant generatedAt;

    protected GeneratedPost() { }  // for JPA

    public GeneratedPost(String channel, String text, Instant generatedAt) {
        this.channel = channel;
        this.text = text;
        this.generatedAt = generatedAt;
    }

    public Long getId() { return id; }
    public String getChannel() { return channel; }
    public String getText() { return text; }
    public Instant getGeneratedAt() { return generatedAt; }
}
