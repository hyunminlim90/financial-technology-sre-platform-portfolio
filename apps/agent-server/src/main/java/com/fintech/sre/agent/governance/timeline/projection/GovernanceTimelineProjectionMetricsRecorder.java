package com.fintech.sre.agent.governance.timeline.projection;

import java.util.Objects;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

public class GovernanceTimelineProjectionMetricsRecorder {

	private final MeterRegistry meterRegistry;

	public GovernanceTimelineProjectionMetricsRecorder(
			MeterRegistry meterRegistry
	) {
		this.meterRegistry = Objects.requireNonNull(
				meterRegistry,
				"meterRegistry must not be null"
		);
	}

	public void write(GovernanceTimelineProjectionWriteStatus status) {
		Objects.requireNonNull(status, "status must not be null");

		Counter.builder(GovernanceTimelineProjectionMetricName.WRITE_TOTAL)
				.tag("result", result(status))
				.register(meterRegistry)
				.increment();
	}

	public void failure() {
		Counter.builder(GovernanceTimelineProjectionMetricName.WRITE_FAILURE_TOTAL)
				.tag("result", "failure")
				.register(meterRegistry)
				.increment();
	}

	private String result(GovernanceTimelineProjectionWriteStatus status) {
		return switch (status) {
			case APPENDED -> "appended";
			case DUPLICATE_SKIPPED -> "duplicate_skipped";
			case REJECTED -> "rejected";
		};
	}
}
