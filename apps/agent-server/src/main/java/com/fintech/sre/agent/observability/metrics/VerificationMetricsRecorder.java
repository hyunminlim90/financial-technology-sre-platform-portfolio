package com.fintech.sre.agent.observability.metrics;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.recommendation.verification.VerificationResultRecord;

@Component
public class VerificationMetricsRecorder {

	private final GovernanceMetricsRecorder recorder;

	public VerificationMetricsRecorder(GovernanceMetricsRecorder recorder) {
		this.recorder = recorder;
	}

	public void recordVerification(VerificationResultRecord record) {
		recorder.increment(
				GovernanceMetricName.VERIFICATION_RESULT,
				Map.of(
						"status", record.status().name(),
						"incidentId", safe(record.incidentId())
				)
		);
	}

	private String safe(String value) {
		return value == null || value.isBlank() ? "unknown" : value;
	}
}
