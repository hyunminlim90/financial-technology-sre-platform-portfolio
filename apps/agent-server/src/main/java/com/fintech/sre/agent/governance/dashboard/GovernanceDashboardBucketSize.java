package com.fintech.sre.agent.governance.dashboard;

import java.time.Duration;

public enum GovernanceDashboardBucketSize {
	FIFTEEN_MINUTES("15m", Duration.ofMinutes(15)),
	ONE_HOUR("1h", Duration.ofHours(1)),
	ONE_DAY("1d", Duration.ofDays(1));

	private final String value;
	private final Duration duration;

	GovernanceDashboardBucketSize(String value, Duration duration) {
		this.value = value;
		this.duration = duration;
	}

	public Duration duration() {
		return duration;
	}

	public static GovernanceDashboardBucketSize from(String value) {
		if (value == null || value.isBlank()) {
			return ONE_HOUR;
		}

		for (GovernanceDashboardBucketSize bucket : values()) {
			if (bucket.value.equalsIgnoreCase(value.trim())) {
				return bucket;
			}
		}

		throw new GovernanceDashboardRejectedException(
				"INVALID_DASHBOARD_BUCKET_SIZE",
				"Supported bucket sizes are 15m, 1h, 1d."
		);
	}
}
