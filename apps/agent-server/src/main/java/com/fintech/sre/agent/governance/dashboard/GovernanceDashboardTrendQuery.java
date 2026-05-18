package com.fintech.sre.agent.governance.dashboard;

import java.time.Instant;

public record GovernanceDashboardTrendQuery(
		String window,
		Instant from,
		Instant to,
		String bucket
) {
	public GovernanceDashboardTimeRange toTimeRange(Instant now) {
		return new GovernanceDashboardQuery(window, from, to)
				.toTimeRange(now);
	}

	public GovernanceDashboardBucketSize toBucketSize() {
		return GovernanceDashboardBucketSize.from(bucket);
	}
}
