package com.fintech.sre.agent.runtime.reliability;

public enum LokiEvidenceRejectionReason {
	LOGS_SOURCE_REQUIRED,
	SENSITIVE_PAYLOAD_FORBIDDEN,
	HIGH_CARDINALITY_LABELS_FORBIDDEN,
	PAYMENT_CONSISTENCY_METADATA_REQUIRED
}
