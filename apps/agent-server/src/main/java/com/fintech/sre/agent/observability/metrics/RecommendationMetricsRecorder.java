package com.fintech.sre.agent.observability.metrics;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.recommendation.persistence.RecommendationRecord;

@Component
public class RecommendationMetricsRecorder {

	private final GovernanceMetricsRecorder recorder;

	public RecommendationMetricsRecorder(GovernanceMetricsRecorder recorder) {
		this.recorder = recorder;
	}

	public void recordCreated(RecommendationRecord record) {
		recorder.increment(
				GovernanceMetricName.RECOMMENDATION_CREATED,
				Map.of(
						"service", safe(record.service()),
						"domain", safe(record.domain()),
						"severity", safe(record.severity()),
						"policyDecision", safe(record.policyDecision()),
						"guardrailDecision", safe(record.guardrailDecision())
				)
		);
	}

	private String safe(String value) {
		return value == null || value.isBlank() ? "unknown" : value;
	}
}
