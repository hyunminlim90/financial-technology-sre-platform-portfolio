package com.fintech.sre.agent.runtime.action;

public enum ActionCommandIntegrationStatus {
	ACTION_COMMAND_CANDIDATE_READY,
	PARTIAL_ACTION_COMMAND,
	NOT_READY,
	UNRELIABLE,
	BLOCKED,
	UNKNOWN
}
