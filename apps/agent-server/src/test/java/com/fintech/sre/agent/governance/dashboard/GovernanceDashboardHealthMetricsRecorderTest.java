package com.fintech.sre.agent.governance.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class GovernanceDashboardHealthMetricsRecorderTest {

	@Test
	void shouldUpdateGaugeValueWhenHealthResponseIsRecorded() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceDashboardHealthMetricsRecorder recorder =
				new GovernanceDashboardHealthMetricsRecorder(registry);

		recorder.record(new GovernanceDashboardHealthResponse(
				Instant.now(),
				GovernanceDashboardHealthStatus.DEGRADED,
				false,
				true,
				true,
				true,
				"optimized_query_repository_missing",
				"degraded"
		));

		assertThat(registry.find(GovernanceDashboardMetricName.HEALTH_STATUS)
				.gauge()
				.value()).isEqualTo(1.0);
	}

	@Test
	void shouldIgnoreNullResponse() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceDashboardHealthMetricsRecorder recorder =
				new GovernanceDashboardHealthMetricsRecorder(registry);

		recorder.record(null);

		assertThat(registry.find(GovernanceDashboardMetricName.HEALTH_STATUS)
				.tag("component", "governance-dashboard")
				.gauge()
				.value()).isEqualTo(2.0);
	}
}
