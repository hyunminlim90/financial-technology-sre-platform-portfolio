package com.fintech.sre.agent.runtime.reliability;

public enum PrometheusEvidenceRejectionReason {
	METRICS_SOURCE_REQUIRED,
	HIGH_CARDINALITY_LABELS_FORBIDDEN,
	PAYMENT_CONSISTENCY_METADATA_REQUIRED
}
