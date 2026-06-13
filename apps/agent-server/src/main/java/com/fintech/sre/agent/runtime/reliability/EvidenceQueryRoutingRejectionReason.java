package com.fintech.sre.agent.runtime.reliability;

public enum EvidenceQueryRoutingRejectionReason {
	NO_SELECTION_AVAILABLE,
	PAYMENT_SUPPORT_REQUIRED,
	UNAVAILABLE_ADAPTER,
	INVALID_ROUTE_SCOPE,
	UNKNOWN
}
