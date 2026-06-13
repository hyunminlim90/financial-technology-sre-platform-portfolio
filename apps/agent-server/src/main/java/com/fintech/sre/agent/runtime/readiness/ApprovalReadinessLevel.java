package com.fintech.sre.agent.runtime.readiness;

public enum ApprovalReadinessLevel {
	READY,
	PARTIAL,
	NOT_READY,
	UNRELIABLE,
	BLOCKED,
	UNKNOWN
}
