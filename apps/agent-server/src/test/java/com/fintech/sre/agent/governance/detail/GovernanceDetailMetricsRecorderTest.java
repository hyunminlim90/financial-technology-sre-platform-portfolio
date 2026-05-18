package com.fintech.sre.agent.governance.detail;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.observability.metrics.GovernanceMetricsRecorder;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class GovernanceDetailMetricsRecorderTest {

	@Test
	void shouldRecordSuccessMetric() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceDetailMetricsRecorder recorder =
				new GovernanceDetailMetricsRecorder(
						new GovernanceMetricsRecorder(registry)
				);

		recorder.success("incident");

		assertThat(registry.find(GovernanceDetailMetricName.QUERY_TOTAL)
				.tag("detailType", "incident")
				.tag("result", "success")
				.counter()
				.count()).isEqualTo(1.0);
	}

	@Test
	void shouldRecordNotFoundMetricsWithoutFailureMetric() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceDetailMetricsRecorder recorder =
				new GovernanceDetailMetricsRecorder(
						new GovernanceMetricsRecorder(registry)
				);

		recorder.notFound("knowledgeUpdate");

		assertThat(registry.find(GovernanceDetailMetricName.QUERY_TOTAL)
				.tag("detailType", "knowledgeUpdate")
				.tag("result", "not_found")
				.counter()
				.count()).isEqualTo(1.0);
		assertThat(registry.find(GovernanceDetailMetricName.QUERY_NOT_FOUND)
				.tag("detailType", "knowledgeUpdate")
				.tag("result", "not_found")
				.counter()
				.count()).isEqualTo(1.0);
		assertThat(registry.find(GovernanceDetailMetricName.QUERY_TOTAL)
				.tag("detailType", "knowledgeUpdate")
				.tag("result", "failure")
				.counter()).isNull();
	}

	@Test
	void shouldRecordFailureMetric() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceDetailMetricsRecorder recorder =
				new GovernanceDetailMetricsRecorder(
						new GovernanceMetricsRecorder(registry)
				);

		recorder.failure("learningCandidate");

		assertThat(registry.find(GovernanceDetailMetricName.QUERY_TOTAL)
				.tag("detailType", "learningCandidate")
				.tag("result", "failure")
				.counter()
				.count()).isEqualTo(1.0);
	}

	@Test
	void shouldRecordDegradedMetricWithReasonAndComponent() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceDetailMetricsRecorder recorder =
				new GovernanceDetailMetricsRecorder(
						new GovernanceMetricsRecorder(registry)
				);

		recorder.degraded("incident", "component_query_timeout", "approvals");

		assertThat(registry.find(GovernanceDetailMetricName.DEGRADED_TOTAL)
				.tag("detailType", "incident")
				.tag("result", "degraded")
				.tag("reason", "component_query_timeout")
				.tag("component", "approvals")
				.counter()
				.count()).isEqualTo(1.0);
	}
}
