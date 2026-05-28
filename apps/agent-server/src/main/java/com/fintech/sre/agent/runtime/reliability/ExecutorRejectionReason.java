package com.fintech.sre.agent.runtime.reliability;

public enum ExecutorRejectionReason {
	READINESS_NOT_ACCEPTED,
	MISSING_AUDIT_TRACE_FOR_PAYMENT_IMPACTING_EXECUTION
}
