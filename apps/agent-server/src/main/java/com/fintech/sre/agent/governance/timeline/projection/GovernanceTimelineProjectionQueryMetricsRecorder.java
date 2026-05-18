package com.fintech.sre.agent.governance.timeline.projection;

import java.util.Objects;

import com.fintech.sre.agent.governance.pagination.GovernanceCursorDirection;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;

public class GovernanceTimelineProjectionQueryMetricsRecorder {

	private final MeterRegistry meterRegistry;

	public GovernanceTimelineProjectionQueryMetricsRecorder(
			MeterRegistry meterRegistry
	) {
		this.meterRegistry = Objects.requireNonNull(
				meterRegistry,
				"meterRegistry must not be null"
		);
	}

	public void query(String result, GovernanceCursorDirection direction) {
		Objects.requireNonNull(result, "result must not be null");
		Objects.requireNonNull(direction, "direction must not be null");

		Counter.builder(GovernanceTimelineProjectionQueryMetricName.QUERY_TOTAL)
				.tag("result", result)
				.tag("direction", direction.name())
				.register(meterRegistry)
				.increment();
	}

	public void failure(GovernanceCursorDirection direction) {
		Objects.requireNonNull(direction, "direction must not be null");

		Counter.builder(GovernanceTimelineProjectionQueryMetricName.QUERY_FAILURE_TOTAL)
				.tag("result", "failure")
				.tag("direction", direction.name())
				.register(meterRegistry)
				.increment();
	}

	public void pageSize(GovernanceCursorDirection direction, int size) {
		Objects.requireNonNull(direction, "direction must not be null");

		DistributionSummary.builder(GovernanceTimelineProjectionQueryMetricName.PAGE_SIZE)
				.tag("direction", direction.name())
				.register(meterRegistry)
				.record(Math.max(size, 0));
	}
}
