package com.fintech.sre.agent.governance.console;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class GovernanceConsoleRuntimeMetricsRecorderTest {

	@Test
	void shouldUpdateGaugeValueWhenRuntimeSummaryIsRecorded() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceConsoleRuntimeMetricsRecorder recorder =
				new GovernanceConsoleRuntimeMetricsRecorder(registry);

		recorder.record(new GovernanceConsoleRuntimeSummaryResponse(
				Instant.now(),
				GovernanceConsoleRuntimeMode.DEGRADED_READ_ONLY,
				null,
				null,
				null,
				null,
				null,
				List.of("dashboard:DEGRADED"),
				"degraded"
		));

		assertThat(registry.find(GovernanceConsoleMetricName.RUNTIME_MODE)
				.tag("component", "governance-console-runtime")
				.gauge()
				.value()).isEqualTo(1.0);
	}

	@Test
	void shouldIgnoreNullResponse() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceConsoleRuntimeMetricsRecorder recorder =
				new GovernanceConsoleRuntimeMetricsRecorder(registry);

		recorder.record(null);

		assertThat(registry.find(GovernanceConsoleMetricName.RUNTIME_MODE)
				.tag("component", "governance-console-runtime")
				.gauge()
				.value()).isEqualTo(2.0);
	}
}
