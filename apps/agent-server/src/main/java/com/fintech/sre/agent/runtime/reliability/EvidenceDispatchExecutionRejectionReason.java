package com.fintech.sre.agent.runtime.reliability;

public enum EvidenceDispatchExecutionRejectionReason {
	DISPATCH_REQUEST_REQUIRED,
	REJECTED_DISPATCH,
	PAYMENT_EVIDENCE_INTEGRITY_REQUIRED,
	NON_NORMALIZED_RESULT,
	UNKNOWN
}
