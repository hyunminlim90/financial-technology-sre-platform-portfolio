package com.fintech.sre.agent.governance.search;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.observability.metrics.GovernanceMetricsRecorder;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;

@Component
public class GovernanceSearchMetricsRecorder {

	private final GovernanceMetricsRecorder counterRecorder;
	private final MeterRegistry meterRegistry;

	public GovernanceSearchMetricsRecorder(
			GovernanceMetricsRecorder counterRecorder,
			MeterRegistry meterRegistry
	) {
		this.counterRecorder = counterRecorder;
		this.meterRegistry = meterRegistry;
	}

	public void success(GovernanceSearchType type, int resultCount) {
		String result = resultCount <= 0 ? "empty" : "success";

		counterRecorder.increment(
				GovernanceSearchMetricName.QUERY_TOTAL,
				tags(type, result)
		);

		DistributionSummary.builder(GovernanceSearchMetricName.RESULT_COUNT)
				.tag("type", safe(type))
				.register(meterRegistry)
				.record(Math.max(0, resultCount));
	}

	public void failure(GovernanceSearchType type) {
		counterRecorder.increment(
				GovernanceSearchMetricName.QUERY_TOTAL,
				tags(type, "failure")
		);
	}

	public void degraded(
			GovernanceSearchType type,
			String reason,
			String component
	) {
		counterRecorder.increment(
				GovernanceSearchMetricName.DEGRADED_TOTAL,
				Map.of(
						"type", safe(type),
						"reason", safe(reason),
						"component", safe(component)
				)
		);
	}

	private Map<String, String> tags(
			GovernanceSearchType type,
			String result
	) {
		return Map.of(
				"type", safe(type),
				"result", safe(result)
		);
	}

	private String safe(GovernanceSearchType type) {
		return type == null ? "UNKNOWN" : type.name();
	}

	private String safe(String value) {
		return value == null || value.isBlank()
				? "unknown"
				: value;
	}
}
