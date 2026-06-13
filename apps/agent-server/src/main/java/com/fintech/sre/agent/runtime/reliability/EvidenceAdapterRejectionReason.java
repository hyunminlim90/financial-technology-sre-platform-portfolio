package com.fintech.sre.agent.runtime.reliability;

public enum EvidenceAdapterRejectionReason {
	DUPLICATE_ADAPTER_ID,
	UNSUPPORTED_SOURCE_TYPE,
	INVALID_DESCRIPTOR,
	DEPRECATED_ADAPTER,
	UNKNOWN
}
