package com.fintech.sre.agent.governance.detail;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GovernanceDetailHealthStatusValueTest {

	@Test
	void shouldMapHealthyToZero() {
		assertThat(GovernanceDetailHealthStatusValue.valueOf(
				GovernanceDetailHealthStatus.HEALTHY
		)).isEqualTo(0.0);
	}

	@Test
	void shouldMapDegradedCapableToOne() {
		assertThat(GovernanceDetailHealthStatusValue.valueOf(
				GovernanceDetailHealthStatus.DEGRADED_CAPABLE
		)).isEqualTo(1.0);
	}

	@Test
	void shouldMapStrictOrNullToTwo() {
		assertThat(GovernanceDetailHealthStatusValue.valueOf(
				GovernanceDetailHealthStatus.STRICT
		)).isEqualTo(2.0);
		assertThat(GovernanceDetailHealthStatusValue.valueOf(null))
				.isEqualTo(2.0);
	}
}
