package com.fintech.sre.agent.runtime.reliability;

public enum EvidenceAdapterSelectionPolicy {
	PREFER_AVAILABLE,
	ALLOW_DEPRECATED_RESTRICTED,
	ALLOW_UNKNOWN_UNCERTAIN,
	REQUIRE_PAYMENT_EVIDENCE_SUPPORT
}
