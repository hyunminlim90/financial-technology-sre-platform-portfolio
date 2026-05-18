package com.fintech.sre.agent.governance.dashboard;

import java.time.Instant;

public record GovernanceDashboardTimeRange(
		Instant from,
		Instant to
) {
	public boolean contains(Instant instant) {
		if (instant == null) {
			return false;
		}

		return !instant.isBefore(from) && !instant.isAfter(to);
	}
}
