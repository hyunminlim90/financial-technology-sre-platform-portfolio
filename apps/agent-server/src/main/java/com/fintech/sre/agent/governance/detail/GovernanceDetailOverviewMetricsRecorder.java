package com.fintech.sre.agent.governance.detail;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.observability.metrics.GovernanceMetricsRecorder;

@Component
public class GovernanceDetailOverviewMetricsRecorder {

	private final GovernanceMetricsRecorder recorder;

	public GovernanceDetailOverviewMetricsRecorder(
			GovernanceMetricsRecorder recorder
	) {
		this.recorder = recorder;
	}

	public void success(String detailType) {
		query(detailType, "success");
	}

	public void notFound(String detailType) {
		query(detailType, "not_found");
	}

	public void failure(String detailType) {
		query(detailType, "failure");
	}

	public void degraded(String detailType, String reason) {
		recorder.increment(
				GovernanceDetailOverviewMetricName.DEGRADED_TOTAL,
				Map.of(
						"detailType", safe(detailType),
						"reason", safe(reason)
				)
		);
	}

	private void query(String detailType, String result) {
		recorder.increment(
				GovernanceDetailOverviewMetricName.QUERY_TOTAL,
				Map.of(
						"detailType", safe(detailType),
						"result", safe(result)
				)
		);
	}

	private String safe(String value) {
		return value == null || value.isBlank()
				? "unknown"
				: value;
	}
}
