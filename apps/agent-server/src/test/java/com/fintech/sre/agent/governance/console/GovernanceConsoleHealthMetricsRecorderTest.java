package com.fintech.sre.agent.governance.console;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class GovernanceConsoleHealthMetricsRecorderTest {

	@Test
	void shouldUpdateGaugeValueWhenHealthResponseIsRecorded() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceConsoleHealthMetricsRecorder recorder =
				new GovernanceConsoleHealthMetricsRecorder(registry);

		recorder.record(new GovernanceConsoleHealthResponse(
				Instant.now(),
				GovernanceConsoleHealthStatus.DEGRADED,
				null,
				null,
				null,
				"degraded"
		));

		assertThat(registry.find(GovernanceConsoleMetricName.HEALTH_STATUS)
				.tag("component", "governance-console")
				.gauge()
				.value()).isEqualTo(1.0);
	}

	@Test
	void shouldIgnoreNullResponse() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceConsoleHealthMetricsRecorder recorder =
				new GovernanceConsoleHealthMetricsRecorder(registry);

		recorder.record(null);

		assertThat(registry.find(GovernanceConsoleMetricName.HEALTH_STATUS)
				.tag("component", "governance-console")
				.gauge()
				.value()).isEqualTo(2.0);
	}
}
