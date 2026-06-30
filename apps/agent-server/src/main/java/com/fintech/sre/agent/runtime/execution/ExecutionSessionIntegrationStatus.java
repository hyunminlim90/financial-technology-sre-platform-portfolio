package com.fintech.sre.agent.runtime.execution;

public enum ExecutionSessionIntegrationStatus {
	EXECUTION_SESSION_READY_VIEW,
	PARTIAL_EXECUTION_SESSION,
	NOT_READY,
	UNRELIABLE,
	BLOCKED,
	UNKNOWN
}
