package com.fintech.sre.agent.runtime.recommendation;

import java.time.Instant;
import java.util.Objects;

public record RecommendationPresentation(
		String recommendationId,
		String title,
		String summary,
		RecommendationModelType recommendationType,
		RecommendationModelReason recommendationReason,
		String scenarioReference,
		String runbookReference,
		String rollbackReference,
		String verificationReference,
		String evidenceReference,
		String paymentSafetyClassification,
		Instant presentationTimestamp,
		RecommendationPresentationStatus status,
		RecommendationPresentationReason reason,
		RecommendationPresentationScope scope
) {
	public RecommendationPresentation {
		requireText(recommendationId, "recommendationId");
		requireText(title, "title");
		requireText(summary, "summary");
		Objects.requireNonNull(
				recommendationType,
				"recommendationType must not be null"
		);
		Objects.requireNonNull(
				recommendationReason,
				"recommendationReason must not be null"
		);
		requireText(scenarioReference, "scenarioReference");
		requireText(runbookReference, "runbookReference");
		requireText(rollbackReference, "rollbackReference");
		requireText(verificationReference, "verificationReference");
		requireText(evidenceReference, "evidenceReference");
		requireText(paymentSafetyClassification, "paymentSafetyClassification");
		Objects.requireNonNull(
				presentationTimestamp,
				"presentationTimestamp must not be null"
		);
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(reason, "reason must not be null");
		Objects.requireNonNull(scope, "scope must not be null");
	}

	public boolean readOnly() {
		return true;
	}

	public boolean recommendationMutation() {
		return false;
	}

	public boolean approvalRequest() {
		return false;
	}

	public boolean actionCommand() {
		return false;
	}

	public boolean executionPermission() {
		return false;
	}

	private static void requireText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
	}
}
