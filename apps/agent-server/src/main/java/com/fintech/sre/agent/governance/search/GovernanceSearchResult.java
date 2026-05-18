package com.fintech.sre.agent.governance.search;

import java.time.Instant;

public record GovernanceSearchResult(
		GovernanceSearchType type,
		String recordId,
		String title,
		String status,
		String summary,
		Instant occurredAt,
		String detailPath,
		String overviewPath
) {
}
