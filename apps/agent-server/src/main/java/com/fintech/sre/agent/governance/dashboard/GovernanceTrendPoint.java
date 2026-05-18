package com.fintech.sre.agent.governance.dashboard;

import java.time.Instant;
import java.util.Map;

public record GovernanceTrendPoint(
		Instant bucketStart,
		Instant bucketEnd,
		long total,
		Map<String, Long> byStatus
) {
}
