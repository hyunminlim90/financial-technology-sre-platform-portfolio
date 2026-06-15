package com.fintech.sre.agent.runtime.approval;

public enum ApprovalRequestIntegrationStatus {
	APPROVAL_REQUEST_READY,
	PARTIAL_APPROVAL_REQUEST,
	NOT_READY,
	UNRELIABLE,
	BLOCKED,
	UNKNOWN
}
