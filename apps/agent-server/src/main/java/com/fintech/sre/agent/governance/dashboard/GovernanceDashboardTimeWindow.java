package com.fintech.sre.agent.governance.dashboard;

import java.time.Duration;

public enum GovernanceDashboardTimeWindow {
	ONE_HOUR("1h", Duration.ofHours(1)),
	TWENTY_FOUR_HOURS("24h", Duration.ofHours(24)),
	SEVEN_DAYS("7d", Duration.ofDays(7));

	private final String value;
	private final Duration duration;

	GovernanceDashboardTimeWindow(String value, Duration duration) {
		this.value = value;
		this.duration = duration;
	}

	public Duration duration() {
		return duration;
	}

	public static GovernanceDashboardTimeWindow from(String value) {
		if (value == null || value.isBlank()) {
			return TWENTY_FOUR_HOURS;
		}

		for (GovernanceDashboardTimeWindow window : values()) {
			if (window.value.equalsIgnoreCase(value.trim())) {
				return window;
			}
		}

		throw new GovernanceDashboardRejectedException(
				"INVALID_DASHBOARD_TIME_WINDOW",
				"Supported windows are 1h, 24h, 7d."
		);
	}
}
