package com.fintech.sre.agent.runtime.recommendation;

public enum RecommendationModelIntegrationStatus {
	RECOMMENDATION_READY,
	PARTIAL_RECOMMENDATION,
	NOT_READY,
	UNRELIABLE,
	BLOCKED,
	UNKNOWN
}
