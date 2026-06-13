package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record EvidenceDispatchExecutionPipelineInput(
		EvidenceDispatchRequest dispatchRequest
) {
	public EvidenceDispatchExecutionPipelineInput {
		Objects.requireNonNull(dispatchRequest, "dispatchRequest must not be null");
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
