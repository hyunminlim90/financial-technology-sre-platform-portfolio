package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.governance.pagination.GovernanceCursorDirection;
import com.fintech.sre.agent.observability.metrics.GovernanceMetricsRecorder;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class GovernanceTimelineMetricsRecorderTest {

	@Test
	void shouldRecordQueryMetrics() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceTimelineMetricsRecorder recorder = recorder(registry);

		recorder.query("success", GovernanceCursorDirection.NEXT);

		assertThat(registry.find(GovernanceTimelineMetricName.QUERY_TOTAL)
				.tag(GovernanceTimelineMetricTag.RESULT, "success")
				.tag("direction", "NEXT")
				.counter()
				.count()).isEqualTo(1.0);
		assertThat(registry.find(GovernanceTimelineMetricName.QUERY_TOTAL)
				.tag("cursor", "opaque")
				.counter()).isNull();
	}

	@Test
	void shouldRecordDegradedMetrics() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceTimelineMetricsRecorder recorder = recorder(registry);

		recorder.degraded(
				GovernanceTimelineResilienceMode.PARTIAL_DEGRADED,
				"timeline_aggregation_degraded",
				GovernanceTimelineAggregationSource.VERIFICATION
		);

		assertThat(registry.find(GovernanceTimelineMetricName.DEGRADED_TOTAL)
				.tag(GovernanceTimelineMetricTag.MODE, "PARTIAL_DEGRADED")
				.tag(GovernanceTimelineMetricTag.REASON, "timeline_aggregation_degraded")
				.tag(GovernanceTimelineMetricTag.SOURCE, "VERIFICATION")
				.counter()
				.count()).isEqualTo(1.0);
		assertThat(registry.find(GovernanceTimelineMetricName.DEGRADED_TOTAL)
				.tag("eventId", "event-1")
				.counter()).isNull();
	}

	@Test
	void shouldRecordPageSizeDistribution() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceTimelineMetricsRecorder recorder = recorder(registry);

		recorder.pageSize(GovernanceTimelineResilienceMode.STRICT, 3);

		assertThat(registry.find(GovernanceTimelineMetricName.PAGE_SIZE)
				.tag(GovernanceTimelineMetricTag.MODE, "STRICT")
				.summary()
				.totalAmount()).isEqualTo(3.0);
	}

	private GovernanceTimelineMetricsRecorder recorder(SimpleMeterRegistry registry) {
		return new GovernanceTimelineMetricsRecorder(
				new GovernanceMetricsRecorder(registry),
				registry
		);
	}
}
