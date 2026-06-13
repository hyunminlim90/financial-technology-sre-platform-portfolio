package com.fintech.sre.agent.runtime.reliability;

public enum EvidenceConfidenceIntegrationReason {
	HIGH_CONFIDENCE,
	MEDIUM_CONFIDENCE,
	LOW_CONFIDENCE,
	INSUFFICIENT_CONFIDENCE,
	PAYMENT_CONFIDENCE_DOWNGRADE,
	CONTRADICTORY_CONFIDENCE,
	UNKNOWN
}
