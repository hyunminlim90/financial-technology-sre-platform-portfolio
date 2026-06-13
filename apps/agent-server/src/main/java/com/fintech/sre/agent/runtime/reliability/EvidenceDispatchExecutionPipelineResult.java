package com.fintech.sre.agent.runtime.reliability;

import java.util.List;
import java.util.Objects;

public record EvidenceDispatchExecutionPipelineResult(
		List<EvidenceDispatchExecutionPipelineStage> stages,
		EvidenceDispatchResult dispatchResult,
		EvidenceDispatchExecutionRequest executionRequest,
		EvidenceDispatchExecutionResponse executionResponse,
		EvidenceCollectionStatus propagatedCollectionStatus,
		boolean paymentSafetyUncertain,
		EvidenceDispatchExecutionPipelineRejectionReason rejectionReason
) {
	public EvidenceDispatchExecutionPipelineResult {
		Objects.requireNonNull(stages, "stages must not be null");
		Objects.requireNonNull(dispatchResult, "dispatchResult must not be null");
		Objects.requireNonNull(
				propagatedCollectionStatus,
				"propagatedCollectionStatus must not be null"
		);
		stages = List.copyOf(stages);
	}

	public boolean exposesRawPayload() {
		return false;
	}

	public boolean recommendation() {
		return false;
	}

	public boolean executionPermission() {
		return false;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}
}
