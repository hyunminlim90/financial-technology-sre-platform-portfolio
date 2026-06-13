package com.fintech.sre.agent.runtime.reliability;

public enum EvidenceSourceType {
	METRICS,
	LOGS,
	TRACES,
	EVENTS,
	DEPLOYMENT,
	ROLLBACK,
	VERIFICATION,
	PAYMENT_CONSISTENCY
}
