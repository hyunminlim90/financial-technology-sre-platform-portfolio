package com.fintech.sre.agent.governance.timeline;

import java.util.List;

import com.fintech.sre.agent.governance.detail.GovernanceDetailTimelineItem;

public record GovernanceTimelinePageResponse(
		List<GovernanceDetailTimelineItem> items,
		GovernanceTimelinePageMetadata page
) {
}
