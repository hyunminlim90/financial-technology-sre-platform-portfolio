package com.fintech.sre.agent.governance.dashboard;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.observability.metrics.GovernanceMetricsRecorder;

@Component
public class GovernanceDashboardMetricsRecorder {

	private final GovernanceMetricsRecorder recorder;

	public GovernanceDashboardMetricsRecorder(
			GovernanceMetricsRecorder recorder
	) {
		this.recorder = recorder;
	}

	public void recordDegradation(
			String endpoint,
			GovernanceDashboardDegradation degradation
	) {
		if (degradation == null || !degradation.degraded()) {
			return;
		}

		recorder.increment(
				GovernanceDashboardMetricName.DEGRADED,
				Map.of(
						"endpoint", safe(endpoint),
						"reason", safe(degradation.reason()),
						"fallbackUsed", String.valueOf(degradation.fallbackUsed())
				)
		);
	}

	private String safe(String value) {
		return value == null || value.isBlank() ? "unknown" : value;
	}
}
