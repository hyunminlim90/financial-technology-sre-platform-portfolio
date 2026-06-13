package com.fintech.sre.agent.runtime.reliability;

public enum EvidenceRoutingPlanRejectionReason {
	NO_ACCEPTED_ROUTE,
	PAYMENT_ROUTE_REQUIRED,
	REJECTED_ROUTE_INCLUDED,
	UNAVAILABLE_ROUTE_INCLUDED,
	UNKNOWN
}
