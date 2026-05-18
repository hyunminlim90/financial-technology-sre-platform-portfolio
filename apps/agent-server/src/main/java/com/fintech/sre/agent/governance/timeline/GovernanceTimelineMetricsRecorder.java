package com.fintech.sre.agent.governance.timeline;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.governance.pagination.GovernanceCursorDirection;
import com.fintech.sre.agent.observability.metrics.GovernanceMetricsRecorder;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;

@Component
public class GovernanceTimelineMetricsRecorder {

	private static final String DIRECTION = "direction";

	private final GovernanceMetricsRecorder counterRecorder;
	private final MeterRegistry meterRegistry;

	public GovernanceTimelineMetricsRecorder(
			GovernanceMetricsRecorder counterRecorder,
			MeterRegistry meterRegistry
	) {
		this.counterRecorder = counterRecorder;
		this.meterRegistry = meterRegistry;
	}

	public void query(String result, GovernanceCursorDirection direction) {
		counterRecorder.increment(
				GovernanceTimelineMetricName.QUERY_TOTAL,
				Map.of(
						GovernanceTimelineMetricTag.RESULT,
						safe(result),
						DIRECTION,
						safe(direction)
				)
		);
	}

	public void degraded(
			GovernanceTimelineResilienceMode mode,
			String reason,
			GovernanceTimelineAggregationSource source
	) {
		counterRecorder.increment(
				GovernanceTimelineMetricName.DEGRADED_TOTAL,
				Map.of(
						GovernanceTimelineMetricTag.MODE,
						safe(mode),
						GovernanceTimelineMetricTag.REASON,
						safe(reason),
						GovernanceTimelineMetricTag.SOURCE,
						safe(source)
				)
		);
	}

	public void pageSize(
			GovernanceTimelineResilienceMode mode,
			int size
	) {
		DistributionSummary.builder(GovernanceTimelineMetricName.PAGE_SIZE)
				.tag(GovernanceTimelineMetricTag.MODE, safe(mode))
				.register(meterRegistry)
				.record(Math.max(0, size));
	}

	private String safe(GovernanceCursorDirection direction) {
		return direction == null ? "unknown" : direction.name();
	}

	private String safe(GovernanceTimelineResilienceMode mode) {
		return mode == null ? "unknown" : mode.name();
	}

	private String safe(GovernanceTimelineAggregationSource source) {
		return source == null ? "unknown" : source.name();
	}

	private String safe(String value) {
		return value == null || value.isBlank()
				? "unknown"
				: value;
	}
}
