package com.fintech.sre.agent.governance.timeline;

import com.fintech.sre.agent.governance.pagination.GovernanceCursorDirection;

public record GovernanceTimelineQuery(
		String cursor,
		Integer limit,
		GovernanceCursorDirection direction,
		GovernanceTimelineFilter filter
) {
	private static final int DEFAULT_LIMIT = 50;
	private static final int MAX_LIMIT = 200;

	public int safeLimit() {
		if (limit == null || limit <= 0) {
			return DEFAULT_LIMIT;
		}

		return Math.min(limit, MAX_LIMIT);
	}

	public GovernanceCursorDirection safeDirection() {
		return direction == null
				? GovernanceCursorDirection.NEXT
				: direction;
	}
}
