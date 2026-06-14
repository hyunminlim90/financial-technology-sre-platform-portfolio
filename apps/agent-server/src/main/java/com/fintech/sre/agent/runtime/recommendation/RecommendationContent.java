package com.fintech.sre.agent.runtime.recommendation;

import java.time.Instant;
import java.util.Objects;

public record RecommendationContent(
		String recommendationId,
		String title,
		String summary,
		RecommendationContentType recommendationType,
		RecommendationContentReason reason,
		RecommendationContentScope scope,
		String scenarioId,
		String runbookId,
		String rollbackId,
		String verificationId,
		String paymentSafetyClassification,
		Instant generatedAt
) {
	public RecommendationContent {
		requireText(recommendationId, "recommendationId");
		requireText(title, "title");
		requireText(summary, "summary");
		Objects.requireNonNull(
				recommendationType,
				"recommendationType must not be null"
		);
		Objects.requireNonNull(reason, "reason must not be null");
		Objects.requireNonNull(scope, "scope must not be null");
		requireText(scenarioId, "scenarioId");
		requireText(runbookId, "runbookId");
		requireText(rollbackId, "rollbackId");
		requireText(verificationId, "verificationId");
		requireText(paymentSafetyClassification, "paymentSafetyClassification");
		Objects.requireNonNull(generatedAt, "generatedAt must not be null");
	}

	public boolean readOnly() {
		return true;
	}

	public boolean llmOutput() {
		return false;
	}

	public boolean ragResult() {
		return false;
	}

	public boolean runbookSourceDocument() {
		return false;
	}

	public boolean actionCommand() {
		return false;
	}

	public boolean approvalRequest() {
		return false;
	}

	public boolean executionPermission() {
		return false;
	}

	public boolean exposesRawPayload() {
		return false;
	}

	public boolean exposesVendorDetail() {
		return false;
	}

	public boolean exposesCredentialConfiguration() {
		return false;
	}

	private static void requireText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
	}
}
