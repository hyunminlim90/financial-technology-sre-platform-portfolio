package com.fintech.sre.agent.governance.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GovernanceDashboardHealthStatusValueTest {

	@Test
	void shouldMapHealthyToZero() {
		assertThat(GovernanceDashboardHealthStatusValue.valueOf(
				GovernanceDashboardHealthStatus.HEALTHY
		)).isEqualTo(0.0);
	}

	@Test
	void shouldMapDegradedToOne() {
		assertThat(GovernanceDashboardHealthStatusValue.valueOf(
				GovernanceDashboardHealthStatus.DEGRADED
		)).isEqualTo(1.0);
	}

	@Test
	void shouldMapUnavailableOrNullToTwo() {
		assertThat(GovernanceDashboardHealthStatusValue.valueOf(
				GovernanceDashboardHealthStatus.UNAVAILABLE
		)).isEqualTo(2.0);
		assertThat(GovernanceDashboardHealthStatusValue.valueOf(null))
				.isEqualTo(2.0);
	}
}
