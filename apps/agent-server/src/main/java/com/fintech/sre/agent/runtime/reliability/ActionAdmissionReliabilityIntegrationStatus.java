package com.fintech.sre.agent.runtime.reliability;

public enum ActionAdmissionReliabilityIntegrationStatus {
	ACTION_ADMISSION_READY,
	PARTIAL_ADMISSION_READINESS,
	WARNING,
	UNRELIABLE,
	BLOCKED,
	UNKNOWN
}
