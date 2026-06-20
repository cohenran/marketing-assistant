package com.duo.marketing.channel;

/**
 * A marketing channel to draft copy for.
 *
 * format       : the structure the draft must follow (title + body, script, ASO, etc.).
 * notes        : tone / rules specific to the channel.
 * cadenceDays  : how often the scheduler auto-generates for this channel.
 *                <= 0 means "manual only" — never auto, generated only via --run-now
 *                (use this for one-shot launches like Product Hunt / Show HN).
 */
public record ChannelTarget(
        String name,
        String format,
        String notes,
        int cadenceDays
) {}
