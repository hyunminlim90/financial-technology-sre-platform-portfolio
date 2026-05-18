package com.fintech.sre.agent.governance.console;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GovernanceConsoleRuntimeModeValueTest {

	@Test
	void shouldMapNormalToZero() {
		assertThat(GovernanceConsoleRuntimeModeValue.valueOf(
				GovernanceConsoleRuntimeMode.NORMAL
		)).isEqualTo(0.0);
	}

	@Test
	void shouldMapDegradedReadOnlyToOne() {
		assertThat(GovernanceConsoleRuntimeModeValue.valueOf(
				GovernanceConsoleRuntimeMode.DEGRADED_READ_ONLY
		)).isEqualTo(1.0);
	}

	@Test
	void shouldMapAttentionRequiredOrNullToTwo() {
		assertThat(GovernanceConsoleRuntimeModeValue.valueOf(
				GovernanceConsoleRuntimeMode.ATTENTION_REQUIRED
		)).isEqualTo(2.0);
		assertThat(GovernanceConsoleRuntimeModeValue.valueOf(null))
				.isEqualTo(2.0);
	}
}
