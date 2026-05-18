package com.fintech.sre.agent.observability.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class GovernanceMetricsRecorderTest {

	@Test
	void shouldRecordCounterWithSafeTags() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceMetricsRecorder recorder = new GovernanceMetricsRecorder(registry);

		recorder.increment(
				GovernanceMetricName.RECOMMENDATION_CREATED,
				Map.of(
						"service", "payment-api",
						"secretToken", "must-not-appear"
				)
		);

		Counter counter = registry.find(GovernanceMetricName.RECOMMENDATION_CREATED)
				.tag("service", "payment-api")
				.counter();

		assertThat(counter).isNotNull();
		assertThat(counter.count()).isEqualTo(1.0);
	}
}
