package com.fintech.sre.agent.governance.query;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.observability.metrics.GovernanceMetricsRecorder;

@Component
public class GovernanceQueryMetricsRecorder {

	private final GovernanceMetricsRecorder recorder;

	public GovernanceQueryMetricsRecorder(
			GovernanceMetricsRecorder recorder
	) {
		this.recorder = recorder;
	}

	public void optimized(String queryType, String series) {
		recorder.increment(
				GovernanceQueryMetricName.OPTIMIZED,
				tags(queryType, series, "optimized", "none")
		);
	}

	public void fallback(
			String queryType,
			String series,
			String reason
	) {
		recorder.increment(
				GovernanceQueryMetricName.FALLBACK,
				tags(queryType, series, "fallback", reason)
		);
	}

	public void failure(
			String queryType,
			String series,
			String reason
	) {
		recorder.increment(
				GovernanceQueryMetricName.FAILURE,
				tags(queryType, series, "failure", reason)
		);
	}

	private Map<String, String> tags(
			String queryType,
			String series,
			String mode,
			String reason
	) {
		return Map.of(
				"queryType", safe(queryType),
				"series", safe(series),
				"mode", safe(mode),
				"reason", safe(reason)
		);
	}

	private String safe(String value) {
		return value == null || value.isBlank() ? "unknown" : value;
	}
}
