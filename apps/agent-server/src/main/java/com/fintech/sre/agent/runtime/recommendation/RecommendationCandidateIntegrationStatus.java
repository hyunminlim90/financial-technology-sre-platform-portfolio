package com.fintech.sre.agent.runtime.recommendation;

public enum RecommendationCandidateIntegrationStatus {
	GENERATION_READY,
	PARTIAL_CANDIDATE,
	NOT_READY,
	UNRELIABLE,
	BLOCKED,
	UNKNOWN
}
