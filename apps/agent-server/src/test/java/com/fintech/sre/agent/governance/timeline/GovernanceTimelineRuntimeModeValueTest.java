package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GovernanceTimelineRuntimeModeValueTest {

	@Test
	void shouldMapRuntimeModeValues() {
		assertThat(GovernanceTimelineRuntimeModeValue.valueOf(
				GovernanceTimelineRuntimeMode.NORMAL
		)).isEqualTo(0.0);
		assertThat(GovernanceTimelineRuntimeModeValue.valueOf(
				GovernanceTimelineRuntimeMode.DEGRADED_READ_ONLY
		)).isEqualTo(1.0);
		assertThat(GovernanceTimelineRuntimeModeValue.valueOf(
				GovernanceTimelineRuntimeMode.ATTENTION_REQUIRED
		)).isEqualTo(2.0);
		assertThat(GovernanceTimelineRuntimeModeValue.valueOf(null)).isEqualTo(2.0);
	}
}
