package com.duo.marketing.promo;

import com.duo.marketing.config.AppProperties;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Sources the approved promotion targets: the curated allow-list from config,
 * plus the Facebook manual fallback (Graph API can't read arbitrary public
 * groups, so we turn each configured THEME into a target the user posts to by hand).
 */
@Service
public class PromotionTargetService {

    private final AppProperties props;

    public PromotionTargetService(AppProperties props) {
        this.props = props;
    }

    public List<PromotionTarget> targets() {
        List<PromotionTarget> all = new ArrayList<>(props.promotionTargets());

        for (String theme : props.facebookThemes()) {
            String search = "https://www.facebook.com/search/groups/?q="
                    + URLEncoder.encode(theme, StandardCharsets.UTF_8);
            all.add(new PromotionTarget(
                    "Facebook group — " + theme,
                    search,
                    "FACEBOOK_GROUP",
                    "Manual fallback: find a group matching this theme and post there. "
                            + "Only post if the group's rules allow self-promotion."
            ));
        }
        return all;
    }
}
