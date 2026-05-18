package com.fintech.sre.agent.governance.detail;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.observability.metrics.GovernanceMetricsRecorder;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class GovernanceDetailOverviewMetricsRecorderTest {

	@Test
	void shouldRecordSuccessMetric() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceDetailOverviewMetricsRecorder recorder =
				new GovernanceDetailOverviewMetricsRecorder(
						new GovernanceMetricsRecorder(registry)
				);

		recorder.success("incident");

		assertThat(registry.find(GovernanceDetailOverviewMetricName.QUERY_TOTAL)
				.tag("detailType", "incident")
				.tag("result", "success")
				.counter()
				.count()).isEqualTo(1.0);
	}

	@Test
	void shouldRecordNotFoundMetric() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceDetailOverviewMetricsRecorder recorder =
				new GovernanceDetailOverviewMetricsRecorder(
						new GovernanceMetricsRecorder(registry)
				);

		recorder.notFound("knowledgeUpdate");

		assertThat(registry.find(GovernanceDetailOverviewMetricName.QUERY_TOTAL)
				.tag("detailType", "knowledgeUpdate")
				.tag("result", "not_found")
				.counter()
				.count()).isEqualTo(1.0);
	}

	@Test
	void shouldRecordFailureMetric() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceDetailOverviewMetricsRecorder recorder =
				new GovernanceDetailOverviewMetricsRecorder(
						new GovernanceMetricsRecorder(registry)
				);

		recorder.failure("learningCandidate");

		assertThat(registry.find(GovernanceDetailOverviewMetricName.QUERY_TOTAL)
				.tag("detailType", "learningCandidate")
				.tag("result", "failure")
				.counter()
				.count()).isEqualTo(1.0);
	}

	@Test
	void shouldRecordDegradedMetric() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceDetailOverviewMetricsRecorder recorder =
				new GovernanceDetailOverviewMetricsRecorder(
						new GovernanceMetricsRecorder(registry)
				);

		recorder.degraded("recommendation", "component_query_failed");

		assertThat(registry.find(GovernanceDetailOverviewMetricName.DEGRADED_TOTAL)
				.tag("detailType", "recommendation")
				.tag("reason", "component_query_failed")
				.counter()
				.count()).isEqualTo(1.0);
	}
}
