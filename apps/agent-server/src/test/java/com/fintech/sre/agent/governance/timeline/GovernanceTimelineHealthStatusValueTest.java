package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GovernanceTimelineHealthStatusValueTest {

	@Test
	void shouldMapHealthStatusValues() {
		assertThat(GovernanceTimelineHealthStatusValue.valueOf(
				GovernanceTimelineHealthStatus.HEALTHY
		)).isEqualTo(0.0);
		assertThat(GovernanceTimelineHealthStatusValue.valueOf(
				GovernanceTimelineHealthStatus.DEGRADED_CAPABLE
		)).isEqualTo(1.0);
		assertThat(GovernanceTimelineHealthStatusValue.valueOf(
				GovernanceTimelineHealthStatus.STRICT
		)).isEqualTo(2.0);
		assertThat(GovernanceTimelineHealthStatusValue.valueOf(
				GovernanceTimelineHealthStatus.UNAVAILABLE
		)).isEqualTo(3.0);
		assertThat(GovernanceTimelineHealthStatusValue.valueOf(null)).isEqualTo(3.0);
	}
}
