package com.fintech.sre.agent.runtime.reliability;

public enum EvidenceAdapterSelectionRejectionReason {
	NO_REGISTERED_ADAPTER,
	NO_AVAILABLE_ADAPTER,
	PAYMENT_EVIDENCE_NOT_SUPPORTED,
	INVALID_SELECTION_SCOPE,
	UNKNOWN
}
