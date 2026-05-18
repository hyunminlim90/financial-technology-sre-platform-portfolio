package com.fintech.sre.agent.governance.detail;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.observability.metrics.GovernanceMetricsRecorder;

@Component
public class GovernanceDetailMetricsRecorder {

	private final GovernanceMetricsRecorder recorder;

	public GovernanceDetailMetricsRecorder(
			GovernanceMetricsRecorder recorder
	) {
		this.recorder = recorder;
	}

	public void success(String detailType) {
		record(GovernanceDetailMetricName.QUERY_TOTAL, detailType, "success");
	}

	public void notFound(String detailType) {
		record(GovernanceDetailMetricName.QUERY_TOTAL, detailType, "not_found");
		record(GovernanceDetailMetricName.QUERY_NOT_FOUND, detailType, "not_found");
	}

	public void failure(String detailType) {
		record(GovernanceDetailMetricName.QUERY_TOTAL, detailType, "failure");
	}

	public void degraded(
			String detailType,
			String reason,
			String component
	) {
		record(
				GovernanceDetailMetricName.DEGRADED_TOTAL,
				detailType,
				"degraded",
				reason,
				component
		);
	}

	private void record(
			String metricName,
			String detailType,
			String result
	) {
		recorder.increment(
				metricName,
				Map.of(
						"detailType", safe(detailType),
						"result", safe(result)
				)
		);
	}

	private void record(
			String metricName,
			String detailType,
			String result,
			String reason,
			String component
	) {
		recorder.increment(
				metricName,
				Map.of(
						"detailType", safe(detailType),
						"result", safe(result),
						"reason", safe(reason),
						"component", safe(component)
				)
		);
	}

	private String safe(String value) {
		return value == null || value.isBlank()
				? "unknown"
				: value;
	}
}
