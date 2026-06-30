package com.fintech.sre.agent.runtime.execution;

public enum ExecutionContextIntegrationStatus {
	EXECUTION_CONTEXT_READY_VIEW,
	PARTIAL_EXECUTION_CONTEXT,
	NOT_READY,
	UNRELIABLE,
	BLOCKED,
	UNKNOWN
}
