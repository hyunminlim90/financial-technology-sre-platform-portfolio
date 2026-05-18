package com.fintech.sre.agent.governance.detail;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class GovernanceDetailHealthMetricsRecorderTest {

	@Test
	void shouldUpdateGaugeValueWhenHealthResponseIsRecorded() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceDetailHealthMetricsRecorder recorder =
				new GovernanceDetailHealthMetricsRecorder(registry);

		recorder.record(new GovernanceDetailHealthResponse(
				Instant.now(),
				GovernanceDetailHealthStatus.DEGRADED_CAPABLE,
				true,
				true,
				true,
				1500,
				"degraded capable"
		));

		assertThat(registry.find(GovernanceDetailMetricName.HEALTH_STATUS)
				.tag("component", "governance-detail")
				.gauge()
				.value()).isEqualTo(1.0);
	}

	@Test
	void shouldIgnoreNullResponse() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceDetailHealthMetricsRecorder recorder =
				new GovernanceDetailHealthMetricsRecorder(registry);

		recorder.record(null);

		assertThat(registry.find(GovernanceDetailMetricName.HEALTH_STATUS)
				.tag("component", "governance-detail")
				.gauge()
				.value()).isEqualTo(2.0);
	}
}
