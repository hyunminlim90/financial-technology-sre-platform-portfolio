package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class GovernanceTimelineHealthResponseTest {

	@Test
	void shouldRetainTimelineHealthFields() {
		GovernanceTimelineHealthResponse response =
				new GovernanceTimelineHealthResponse(
						Instant.parse("2026-05-13T00:00:00Z"),
						GovernanceTimelineHealthStatus.DEGRADED_CAPABLE,
						GovernanceTimelineResilienceMode.PARTIAL_DEGRADED,
						true,
						true,
						true,
						List.of(
								"component_query_failed",
								"component_query_timeout",
								"projection_failed",
								"aggregation_degraded",
								"timeline_query_timeout"
						),
						"Timeline can return partial degraded read-only responses."
				);

		assertThat(response.status()).isEqualTo(
				GovernanceTimelineHealthStatus.DEGRADED_CAPABLE
		);
		assertThat(response.resilienceMode()).isEqualTo(
				GovernanceTimelineResilienceMode.PARTIAL_DEGRADED
		);
		assertThat(response.partialTimelineSupported()).isTrue();
		assertThat(response.failOpenReadOnly()).isTrue();
		assertThat(response.streamingCompatible()).isTrue();
		assertThat(response.degradedReasonTaxonomy()).containsExactly(
				"component_query_failed",
				"component_query_timeout",
				"projection_failed",
				"aggregation_degraded",
				"timeline_query_timeout"
		);
		assertThat(response.message()).contains("partial degraded");
	}
}
