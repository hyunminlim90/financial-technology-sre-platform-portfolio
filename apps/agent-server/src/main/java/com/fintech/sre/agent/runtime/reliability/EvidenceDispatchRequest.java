package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record EvidenceDispatchRequest(
		EvidenceRoutingPlan routingPlan
) {
	public EvidenceDispatchRequest {
		Objects.requireNonNull(routingPlan, "routingPlan must not be null");

		if (!routingPlan.accepted()) {
			throw new IllegalArgumentException(
					EvidenceDispatchRejectionReason.REJECTED_ROUTING_PLAN.name()
			);
		}
		if (routingPlan.paymentConsistencyRequired()
				&& routingPlan.routes().stream().noneMatch(route ->
						route.scope() == EvidenceQueryRoutingScope.PAYMENT_CONSISTENCY_ROUTE
								&& route.paymentSupporting())) {
			throw new IllegalArgumentException(
					EvidenceDispatchRejectionReason.PAYMENT_ROUTE_REQUIRED.name()
			);
		}
	}

	public boolean dispatchCandidate() {
		return true;
	}

	public boolean recommendationAuthority() {
		return false;
	}

	public boolean executionAuthority() {
		return false;
	}

	public boolean exposesRawCredentialOrConfiguration() {
		return false;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}
}
