package com.fintech.sre.agent.runtime.reliability;

public enum EvidenceRuntimeApiRejectionReason {
	MISSING_SUMMARY_RESOURCE,
	UNTRUSTED_AUDIT,
	RAW_PAYLOAD_EXPOSURE_BLOCKED,
	VENDOR_DETAIL_EXPOSURE_BLOCKED,
	CREDENTIAL_EXPOSURE_BLOCKED,
	UNKNOWN
}
