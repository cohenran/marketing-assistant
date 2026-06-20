package com.duo.marketing.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GeneratedPostRepository extends JpaRepository<GeneratedPost, Long> {

    /** Most recent generation for a channel — used to decide if it's due again. */
    Optional<GeneratedPost> findTopByChannelOrderByGeneratedAtDesc(String channel);

    /** Last few drafts for a channel — fed back into the prompt to avoid repetition. */
    List<GeneratedPost> findTop3ByChannelOrderByGeneratedAtDesc(String channel);
}
