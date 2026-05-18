package com.fintech.sre.agent.governance.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class GovernanceDashboardQueryTest {

	@Test
	void shouldBuildDefaultTwentyFourHourRange() {
		Instant now = Instant.parse("2026-05-08T12:00:00Z");

		GovernanceDashboardTimeRange range =
				new GovernanceDashboardQuery(null, null, null).toTimeRange(now);

		assertThat(range.from()).isEqualTo(now.minusSeconds(24 * 60 * 60));
		assertThat(range.to()).isEqualTo(now);
	}

	@Test
	void shouldRejectWhenOnlyFromProvided() {
		assertThatThrownBy(() -> new GovernanceDashboardQuery(
				null,
				Instant.parse("2026-05-08T00:00:00Z"),
				null
		).toTimeRange(Instant.now()))
				.isInstanceOf(GovernanceDashboardRejectedException.class)
				.hasMessage("Both from and to must be provided together.");
	}

	@Test
	void shouldRejectWhenFromAfterTo() {
		assertThatThrownBy(() -> new GovernanceDashboardQuery(
				null,
				Instant.parse("2026-05-09T00:00:00Z"),
				Instant.parse("2026-05-08T00:00:00Z")
		).toTimeRange(Instant.now()))
				.isInstanceOf(GovernanceDashboardRejectedException.class)
				.hasMessage("from must be before to.");
	}
}
