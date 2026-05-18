package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class GovernanceTimelineRuntimeMetricsRecorderTest {

	@Test
	void shouldRecordRuntimeGauge() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceTimelineRuntimeMetricsRecorder recorder =
				new GovernanceTimelineRuntimeMetricsRecorder(registry);

		recorder.record(new GovernanceTimelineRuntimeSummaryResponse(
				Instant.parse("2026-05-15T00:00:00Z"),
				GovernanceTimelineRuntimeMode.DEGRADED_READ_ONLY,
				GovernanceTimelineHealthStatus.DEGRADED_CAPABLE,
				GovernanceTimelineResilienceMode.PARTIAL_DEGRADED,
				true,
				true,
				true,
				List.of("timeline:DEGRADED_CAPABLE"),
				"degraded"
		));

		assertThat(registry.find(GovernanceTimelineMetricName.RUNTIME_MODE)
				.tag("component", "governance-timeline-runtime")
				.gauge()
				.value()).isEqualTo(1.0);
	}

	@Test
	void shouldIgnoreNullResponse() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceTimelineRuntimeMetricsRecorder recorder =
				new GovernanceTimelineRuntimeMetricsRecorder(registry);

		recorder.record(null);

		assertThat(registry.find(GovernanceTimelineMetricName.RUNTIME_MODE)
				.tag("component", "governance-timeline-runtime")
				.gauge()
				.value()).isEqualTo(2.0);
	}
}
