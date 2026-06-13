package com.fintech.sre.agent.runtime.reliability;

public enum EvidenceTrustIntegrationReason {
	HIGH_TRUST_SCORE,
	MEDIUM_TRUST_SCORE,
	LOW_TRUST_SCORE,
	UNTRUSTED_SCORE,
	PAYMENT_TRUST_RESTRICTED,
	BLOCKED_EVIDENCE,
	UNKNOWN
}
