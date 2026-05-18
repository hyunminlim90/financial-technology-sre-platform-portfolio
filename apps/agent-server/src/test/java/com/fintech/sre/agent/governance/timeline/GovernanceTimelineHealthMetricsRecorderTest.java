package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class GovernanceTimelineHealthMetricsRecorderTest {

	@Test
	void shouldRecordHealthGauge() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceTimelineHealthMetricsRecorder recorder =
				new GovernanceTimelineHealthMetricsRecorder(registry);

		recorder.record(new GovernanceTimelineHealthResponse(
				Instant.parse("2026-05-15T00:00:00Z"),
				GovernanceTimelineHealthStatus.HEALTHY,
				GovernanceTimelineResilienceMode.STRICT,
				false,
				false,
				true,
				List.of("component_query_failed"),
				"healthy"
		));

		assertThat(registry.find(GovernanceTimelineMetricName.HEALTH_STATUS)
				.tag("component", "governance-timeline")
				.gauge()
				.value()).isEqualTo(0.0);
	}

	@Test
	void shouldIgnoreNullResponse() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceTimelineHealthMetricsRecorder recorder =
				new GovernanceTimelineHealthMetricsRecorder(registry);

		recorder.record(null);

		assertThat(registry.find(GovernanceTimelineMetricName.HEALTH_STATUS)
				.tag("component", "governance-timeline")
				.gauge()
				.value()).isEqualTo(3.0);
	}
}
