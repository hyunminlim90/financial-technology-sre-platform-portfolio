package com.fintech.sre.agent.governance.query;

import java.time.Instant;

public record GovernanceDashboardTimeBucketResult(
		Instant bucketStart,
		String name,
		long count
) {
}
