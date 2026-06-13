package com.fintech.sre.agent.runtime.reliability;

import java.util.List;
import java.util.Objects;

public record EvidenceExecutionObservablePipelineResult(
		List<EvidenceExecutionObservablePipelineStage> stages,
		EvidenceDispatchExecutionPipelineResult dispatchExecutionPipelineResult,
		ObservableReliabilityRuntimeResult observableRuntimeResult,
		EvidenceExecutionObservablePipelineRejectionReason rejectionReason
) {
	public EvidenceExecutionObservablePipelineResult {
		Objects.requireNonNull(stages, "stages must not be null");
		Objects.requireNonNull(
				dispatchExecutionPipelineResult,
				"dispatchExecutionPipelineResult must not be null"
		);
		Objects.requireNonNull(
				observableRuntimeResult,
				"observableRuntimeResult must not be null"
		);
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
