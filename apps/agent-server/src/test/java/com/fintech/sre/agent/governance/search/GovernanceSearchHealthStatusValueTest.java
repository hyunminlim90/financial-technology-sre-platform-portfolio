package com.fintech.sre.agent.governance.search;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GovernanceSearchHealthStatusValueTest {

	@Test
	void shouldMapHealthyToZero() {
		assertThat(GovernanceSearchHealthStatusValue.valueOf(
				GovernanceSearchHealthStatus.HEALTHY
		)).isEqualTo(0.0);
	}

	@Test
	void shouldMapDegradedCapableToOne() {
		assertThat(GovernanceSearchHealthStatusValue.valueOf(
				GovernanceSearchHealthStatus.DEGRADED_CAPABLE
		)).isEqualTo(1.0);
	}

	@Test
	void shouldMapStrictOrNullToTwo() {
		assertThat(GovernanceSearchHealthStatusValue.valueOf(
				GovernanceSearchHealthStatus.STRICT
		)).isEqualTo(2.0);
		assertThat(GovernanceSearchHealthStatusValue.valueOf(null))
				.isEqualTo(2.0);
	}
}
