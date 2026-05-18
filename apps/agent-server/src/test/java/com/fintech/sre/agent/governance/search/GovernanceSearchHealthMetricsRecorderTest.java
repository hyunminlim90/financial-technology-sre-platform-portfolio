package com.fintech.sre.agent.governance.search;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class GovernanceSearchHealthMetricsRecorderTest {

	@Test
	void shouldUpdateGaugeValueWhenHealthResponseIsRecorded() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceSearchHealthMetricsRecorder recorder =
				new GovernanceSearchHealthMetricsRecorder(registry);

		recorder.record(new GovernanceSearchHealthResponse(
				Instant.now(),
				GovernanceSearchHealthStatus.DEGRADED_CAPABLE,
				true,
				true,
				true,
				1500,
				"degraded capable"
		));

		assertThat(registry.find(GovernanceSearchMetricName.HEALTH_STATUS)
				.tag("component", "governance-search")
				.gauge()
				.value()).isEqualTo(1.0);
	}

	@Test
	void shouldIgnoreNullResponse() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceSearchHealthMetricsRecorder recorder =
				new GovernanceSearchHealthMetricsRecorder(registry);

		recorder.record(null);

		assertThat(registry.find(GovernanceSearchMetricName.HEALTH_STATUS)
				.tag("component", "governance-search")
				.gauge()
				.value()).isEqualTo(2.0);
	}
}
