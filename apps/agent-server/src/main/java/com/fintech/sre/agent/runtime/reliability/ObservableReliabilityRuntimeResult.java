package com.fintech.sre.agent.runtime.reliability;

import java.util.List;
import java.util.Objects;

public record ObservableReliabilityRuntimeResult(
		List<ObservableReliabilityRuntimeStage> stages,
		EvidenceCollectionResult collectionResult,
		EvidenceAssessmentPipelineResult assessmentPipelineResult,
		AssessmentLifecyclePipelineResult assessmentLifecyclePipelineResult,
		ReliabilityLifecycleSummaryResponse summaryResponse,
		ObservableReliabilityRuntimeRejectionReason rejectionReason
) {
	public ObservableReliabilityRuntimeResult {
		Objects.requireNonNull(stages, "stages must not be null");
		Objects.requireNonNull(
				collectionResult,
				"collectionResult must not be null"
		);
		Objects.requireNonNull(
				assessmentPipelineResult,
				"assessmentPipelineResult must not be null"
		);
		Objects.requireNonNull(
				assessmentLifecyclePipelineResult,
				"assessmentLifecyclePipelineResult must not be null"
		);
		Objects.requireNonNull(summaryResponse, "summaryResponse must not be null");
		stages = List.copyOf(stages);
	}

	public boolean readOnly() {
		return true;
	}

	public boolean recommendation() {
		return false;
	}

	public boolean executionPermission() {
		return false;
	}

	public boolean exposesRawPayload() {
		return false;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}
}
