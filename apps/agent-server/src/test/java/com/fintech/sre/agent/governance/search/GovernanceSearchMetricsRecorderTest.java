package com.fintech.sre.agent.governance.search;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.observability.metrics.GovernanceMetricsRecorder;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class GovernanceSearchMetricsRecorderTest {

	@Test
	void shouldRecordSuccessMetricAndResultCount() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceSearchMetricsRecorder recorder =
				new GovernanceSearchMetricsRecorder(
						new GovernanceMetricsRecorder(registry),
						registry
				);

		recorder.success(GovernanceSearchType.ALL, 3);

		assertThat(registry.find(GovernanceSearchMetricName.QUERY_TOTAL)
				.tag("type", "ALL")
				.tag("result", "success")
				.counter()
				.count()).isEqualTo(1.0);
		assertThat(registry.find(GovernanceSearchMetricName.RESULT_COUNT)
				.tag("type", "ALL")
				.summary()
				.count()).isEqualTo(1L);
		assertThat(registry.find(GovernanceSearchMetricName.RESULT_COUNT)
				.tag("type", "ALL")
				.summary()
				.totalAmount()).isEqualTo(3.0);
	}

	@Test
	void shouldRecordEmptyMetric() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceSearchMetricsRecorder recorder =
				new GovernanceSearchMetricsRecorder(
						new GovernanceMetricsRecorder(registry),
						registry
				);

		recorder.success(GovernanceSearchType.INCIDENT, 0);

		assertThat(registry.find(GovernanceSearchMetricName.QUERY_TOTAL)
				.tag("type", "INCIDENT")
				.tag("result", "empty")
				.counter()
				.count()).isEqualTo(1.0);
	}

	@Test
	void shouldRecordFailureMetric() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceSearchMetricsRecorder recorder =
				new GovernanceSearchMetricsRecorder(
						new GovernanceMetricsRecorder(registry),
						registry
				);

		recorder.failure(GovernanceSearchType.KNOWLEDGE_UPDATE);

		assertThat(registry.find(GovernanceSearchMetricName.QUERY_TOTAL)
				.tag("type", "KNOWLEDGE_UPDATE")
				.tag("result", "failure")
				.counter()
				.count()).isEqualTo(1.0);
	}

	@Test
	void shouldRecordDegradedMetric() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceSearchMetricsRecorder recorder =
				new GovernanceSearchMetricsRecorder(
						new GovernanceMetricsRecorder(registry),
						registry
				);

		recorder.degraded(
				GovernanceSearchType.ALL,
				"component_query_failed",
				"incident"
		);

		assertThat(registry.find(GovernanceSearchMetricName.DEGRADED_TOTAL)
				.tag("type", "ALL")
				.tag("reason", "component_query_failed")
				.tag("component", "incident")
				.counter()
				.count()).isEqualTo(1.0);
		assertThat(registry.find(GovernanceSearchMetricName.DEGRADED_TOTAL)
				.tag("q", "incident-1")
				.counter()).isNull();
		assertThat(registry.find(GovernanceSearchMetricName.DEGRADED_TOTAL)
				.tag("recordId", "rec-1")
				.counter()).isNull();
	}
}
