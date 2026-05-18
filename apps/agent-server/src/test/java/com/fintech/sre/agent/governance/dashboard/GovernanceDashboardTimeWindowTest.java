package com.fintech.sre.agent.governance.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class GovernanceDashboardTimeWindowTest {

	@Test
	void shouldParseOneHour() {
		assertThat(GovernanceDashboardTimeWindow.from("1h"))
				.isEqualTo(GovernanceDashboardTimeWindow.ONE_HOUR);
	}

	@Test
	void shouldDefaultToTwentyFourHours() {
		assertThat(GovernanceDashboardTimeWindow.from(null))
				.isEqualTo(GovernanceDashboardTimeWindow.TWENTY_FOUR_HOURS);
	}

	@Test
	void shouldParseSevenDays() {
		assertThat(GovernanceDashboardTimeWindow.from("7d"))
				.isEqualTo(GovernanceDashboardTimeWindow.SEVEN_DAYS);
	}

	@Test
	void shouldRejectInvalidWindow() {
		assertThatThrownBy(() -> GovernanceDashboardTimeWindow.from("30d"))
				.isInstanceOf(GovernanceDashboardRejectedException.class)
				.hasMessage("Supported windows are 1h, 24h, 7d.");
	}
}
