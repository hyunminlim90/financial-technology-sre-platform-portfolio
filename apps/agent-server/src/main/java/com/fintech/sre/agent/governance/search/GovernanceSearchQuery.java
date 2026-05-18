package com.fintech.sre.agent.governance.search;

public record GovernanceSearchQuery(
		String q,
		GovernanceSearchType type,
		String window,
		Integer limit
) {
	public int safeLimit() {
		if (limit == null || limit <= 0) {
			return 20;
		}

		return Math.min(limit, 100);
	}

	public String normalizedQuery() {
		return q == null ? "" : q.trim();
	}
}
