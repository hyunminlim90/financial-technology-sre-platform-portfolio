package com.fintech.sre.agent.governance.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.observability.metrics.GovernanceMetricsRecorder;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class GovernanceDashboardMetricsRecorderTest {

	@Test
	void shouldRecordOnlyDegradedResponses() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceDashboardMetricsRecorder recorder =
				new GovernanceDashboardMetricsRecorder(
						new GovernanceMetricsRecorder(registry)
				);

		recorder.recordDegradation("summary", GovernanceDashboardDegradation.none());
		recorder.recordDegradation(
				"summary",
				GovernanceDashboardDegradation.fallback("query_timeout")
		);

		assertThat(registry.find(GovernanceDashboardMetricName.DEGRADED)
				.tag("endpoint", "summary")
				.tag("reason", "query_timeout")
				.tag("fallbackUsed", "true")
				.counter()
				.count()).isEqualTo(1.0);
	}
}
