package com.fintech.sre.agent.runtime.recommendation;

import java.util.Objects;

public record RecommendationGeneration(
		RecommendationGenerationLevel level,
		RecommendationGenerationReason reason,
		RecommendationGenerationScope scope,
		RecommendationContentIntegrationResult contentIntegrationResult
) {
	public RecommendationGeneration {
		Objects.requireNonNull(level, "level must not be null");
		Objects.requireNonNull(reason, "reason must not be null");
		Objects.requireNonNull(scope, "scope must not be null");
		Objects.requireNonNull(
				contentIntegrationResult,
				"contentIntegrationResult must not be null"
		);
	}

	public boolean readOnly() {
		return true;
	}

	public boolean recommendationEngine() {
		return false;
	}

	public boolean llm() {
		return false;
	}

	public boolean rag() {
		return false;
	}

	public boolean runbookSelector() {
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
}
