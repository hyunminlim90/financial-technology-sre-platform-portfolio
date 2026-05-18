package com.fintech.sre.agent.governance.console;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GovernanceConsoleHealthStatusValueTest {

	@Test
	void shouldMapHealthyToZero() {
		assertThat(GovernanceConsoleHealthStatusValue.valueOf(
				GovernanceConsoleHealthStatus.HEALTHY
		)).isEqualTo(0.0);
	}

	@Test
	void shouldMapDegradedToOne() {
		assertThat(GovernanceConsoleHealthStatusValue.valueOf(
				GovernanceConsoleHealthStatus.DEGRADED
		)).isEqualTo(1.0);
	}

	@Test
	void shouldMapAttentionRequiredOrNullToTwo() {
		assertThat(GovernanceConsoleHealthStatusValue.valueOf(
				GovernanceConsoleHealthStatus.ATTENTION_REQUIRED
		)).isEqualTo(2.0);
		assertThat(GovernanceConsoleHealthStatusValue.valueOf(null))
				.isEqualTo(2.0);
	}
}
