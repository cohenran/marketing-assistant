package com.duo.marketing.llm;

import com.duo.marketing.images.PexelsImage;

import java.util.List;

/**
 * A generated draft for one channel.
 * text      : the post copy (clean — this is what's saved to history).
 * images    : suggested Pexels stock photos (may be empty).
 * htmlPaste : whether this channel accepts pasted HTML (drives email rendering).
 */
public record ChannelDraft(
        String channelName,
        String text,
        List<PexelsImage> images,
        boolean htmlPaste
) {}
