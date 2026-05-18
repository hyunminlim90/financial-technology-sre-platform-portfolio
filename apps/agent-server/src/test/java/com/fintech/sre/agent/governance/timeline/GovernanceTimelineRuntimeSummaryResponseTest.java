package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class GovernanceTimelineRuntimeSummaryResponseTest {

	@Test
	void shouldRetainRuntimeSummaryFields() {
		GovernanceTimelineRuntimeSummaryResponse response =
				new GovernanceTimelineRuntimeSummaryResponse(
						Instant.parse("2026-05-13T00:00:00Z"),
						GovernanceTimelineRuntimeMode.DEGRADED_READ_ONLY,
						GovernanceTimelineHealthStatus.DEGRADED_CAPABLE,
						GovernanceTimelineResilienceMode.FAIL_OPEN_READ_ONLY,
						true,
						true,
						true,
						List.of(
								"timeline:DEGRADED_CAPABLE",
								"timeline:FAIL_OPEN_READ_ONLY"
						),
						"Timeline remains available with partial degraded read-only semantics."
				);

		assertThat(response.runtimeMode()).isEqualTo(
				GovernanceTimelineRuntimeMode.DEGRADED_READ_ONLY
		);
		assertThat(response.healthStatus()).isEqualTo(
				GovernanceTimelineHealthStatus.DEGRADED_CAPABLE
		);
		assertThat(response.resilienceMode()).isEqualTo(
				GovernanceTimelineResilienceMode.FAIL_OPEN_READ_ONLY
		);
		assertThat(response.partialTimelineSupported()).isTrue();
		assertThat(response.failOpenReadOnly()).isTrue();
		assertThat(response.streamingCompatible()).isTrue();
		assertThat(response.degradedSignals()).containsExactly(
				"timeline:DEGRADED_CAPABLE",
				"timeline:FAIL_OPEN_READ_ONLY"
		);
		assertThat(response.message()).contains("partial degraded");
	}
}
