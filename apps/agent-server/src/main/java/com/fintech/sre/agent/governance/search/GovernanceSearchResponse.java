package com.fintech.sre.agent.governance.search;

import java.time.Instant;
import java.util.List;

public record GovernanceSearchResponse(
		Instant searchedAt,
		String query,
		GovernanceSearchType type,
		int limit,
		List<GovernanceSearchResult> results,
		GovernanceSearchDegradation degradation
) {
}
