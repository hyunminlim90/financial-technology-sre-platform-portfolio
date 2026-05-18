package com.fintech.sre.agent.governance.query;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.observability.metrics.GovernanceMetricsRecorder;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class GovernanceQueryMetricsRecorderTest {

	@Test
	void shouldRecordOptimizedFallbackAndFailureMetricsWithSafeTags() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceQueryMetricsRecorder recorder =
				new GovernanceQueryMetricsRecorder(new GovernanceMetricsRecorder(registry));

		recorder.optimized("summary", "approvalStatus");
		recorder.fallback("trend", "incidentLifecycleTransitions", "repository_missing");
		recorder.failure("summary", "verificationStatus", "query_failed");
		recorder.fallback("summary", "secretTokenSeries", "customer_payload");

		assertThat(registry.find(GovernanceQueryMetricName.OPTIMIZED)
				.tag("queryType", "summary")
				.tag("series", "approvalStatus")
				.counter()
				.count()).isEqualTo(1.0);

		assertThat(registry.find(GovernanceQueryMetricName.FALLBACK)
				.tag("queryType", "trend")
				.tag("series", "incidentLifecycleTransitions")
				.tag("reason", "repository_missing")
				.counter()
				.count()).isEqualTo(1.0);

		assertThat(registry.find(GovernanceQueryMetricName.FAILURE)
				.tag("queryType", "summary")
				.tag("series", "verificationStatus")
				.tag("reason", "query_failed")
				.counter()
				.count()).isEqualTo(1.0);

		assertThat(registry.find(GovernanceQueryMetricName.FALLBACK)
				.tag("queryType", "summary")
				.tag("series", "secretTokenSeries")
				.tag("reason", "customer_payload")
				.counter()).isNotNull();
	}
}
