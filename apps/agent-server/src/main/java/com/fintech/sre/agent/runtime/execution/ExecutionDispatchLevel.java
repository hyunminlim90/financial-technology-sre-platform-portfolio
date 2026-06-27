package com.fintech.sre.agent.runtime.execution;

public enum ExecutionDispatchLevel {
	DISPATCH_READY,
	PARTIAL,
	NOT_READY,
	UNRELIABLE,
	BLOCKED,
	UNKNOWN
}
