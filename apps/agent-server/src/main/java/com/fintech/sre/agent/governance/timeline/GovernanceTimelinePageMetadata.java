package com.fintech.sre.agent.governance.timeline;

import java.util.List;

import com.fintech.sre.agent.governance.pagination.GovernanceCursorDirection;

public record GovernanceTimelinePageMetadata(
		String nextCursor,
		String previousCursor,
		boolean hasNext,
		boolean hasPrevious,
		int limit,
		GovernanceCursorDirection direction,
		String ordering,
		boolean degraded,
		List<String> failedComponents
) {
}
