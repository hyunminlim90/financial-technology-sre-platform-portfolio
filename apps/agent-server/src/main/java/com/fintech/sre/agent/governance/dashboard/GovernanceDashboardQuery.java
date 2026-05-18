package com.fintech.sre.agent.governance.dashboard;

import java.time.Instant;

public record GovernanceDashboardQuery(
		String window,
		Instant from,
		Instant to
) {
	public GovernanceDashboardTimeRange toTimeRange(Instant now) {
		if (from != null || to != null) {
			if (from == null || to == null) {
				throw new GovernanceDashboardRejectedException(
						"INVALID_DASHBOARD_TIME_RANGE",
						"Both from and to must be provided together."
				);
			}

			if (from.isAfter(to)) {
				throw new GovernanceDashboardRejectedException(
						"INVALID_DASHBOARD_TIME_RANGE",
						"from must be before to."
				);
			}

			return new GovernanceDashboardTimeRange(from, to);
		}

		GovernanceDashboardTimeWindow parsed =
				GovernanceDashboardTimeWindow.from(window);

		return new GovernanceDashboardTimeRange(
				now.minus(parsed.duration()),
				now
		);
	}
}
