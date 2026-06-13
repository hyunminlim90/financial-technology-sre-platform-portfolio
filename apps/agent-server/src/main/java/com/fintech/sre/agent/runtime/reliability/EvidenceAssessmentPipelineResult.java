package com.fintech.sre.agent.runtime.reliability;

import java.util.List;
import java.util.Objects;

public record EvidenceAssessmentPipelineResult(
		List<EvidenceAssessmentPipelineStage> stages,
		EvidenceCollectionResult collectionResult,
		EvidenceCorrelation evidenceCorrelation,
		ReliabilityAssessmentResult assessmentResult,
		EvidenceAssessmentPipelineRejectionReason rejectionReason
) {
	public EvidenceAssessmentPipelineResult {
		Objects.requireNonNull(stages, "stages must not be null");
		Objects.requireNonNull(
				collectionResult,
				"collectionResult must not be null"
		);
		Objects.requireNonNull(
				evidenceCorrelation,
				"evidenceCorrelation must not be null"
		);
		Objects.requireNonNull(
				assessmentResult,
				"assessmentResult must not be null"
		);
		stages = List.copyOf(stages);
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
