package com.fintech.sre.agent.runtime.verification;

public enum VerificationRequestIntegrationStatus {
	VERIFICATION_REQUEST_READY,
	PARTIAL_VERIFICATION_REQUEST,
	NOT_READY,
	UNRELIABLE,
	BLOCKED,
	UNKNOWN
}
