package com.fintech.sre.agent.runtime.reliability;

public interface EvidenceDispatchExecutorPort {

	EvidenceDispatchExecutionResponse execute(
			EvidenceDispatchExecutionRequest request
	);

	default boolean interfaceContractOnly() {
		return true;
	}

	default boolean recommendationAuthority() {
		return false;
	}

	default boolean actionExecutionAuthority() {
		return false;
	}

	default boolean systemFailure() {
		return false;
	}

	default boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}
}
