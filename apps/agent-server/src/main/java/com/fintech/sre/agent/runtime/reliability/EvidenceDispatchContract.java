package com.fintech.sre.agent.runtime.reliability;

public interface EvidenceDispatchContract {

	EvidenceDispatchResult dispatch(EvidenceDispatchRequest request);

	default boolean executesAdapters() {
		return false;
	}

	default boolean recommendationAuthority() {
		return false;
	}

	default boolean executionAuthority() {
		return false;
	}

	default boolean systemFailure() {
		return false;
	}

	default boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}
}
