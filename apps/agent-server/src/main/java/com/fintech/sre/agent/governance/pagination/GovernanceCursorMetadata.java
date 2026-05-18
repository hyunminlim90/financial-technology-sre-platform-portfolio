package com.fintech.sre.agent.governance.pagination;

public record GovernanceCursorMetadata(
		String nextCursor,
		String previousCursor,
		boolean hasNext,
		boolean hasPrevious,
		int limit,
		GovernanceCursorDirection direction,
		String ordering
) {
}
