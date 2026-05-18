package com.fintech.sre.agent.observability.metrics;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalRecord;

@Component
public class ApprovalMetricsRecorder {

	private final GovernanceMetricsRecorder recorder;

	public ApprovalMetricsRecorder(GovernanceMetricsRecorder recorder) {
		this.recorder = recorder;
	}

	public void recordDecision(RecommendationApprovalRecord record) {
		recorder.increment(
				GovernanceMetricName.RECOMMENDATION_APPROVAL_DECISION,
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
