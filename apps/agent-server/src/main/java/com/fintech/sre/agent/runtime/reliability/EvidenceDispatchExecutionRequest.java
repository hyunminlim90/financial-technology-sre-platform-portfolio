package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record EvidenceDispatchExecutionRequest(
		EvidenceDispatchResult dispatchResult
) {
	public EvidenceDispatchExecutionRequest {
		Objects.requireNonNull(dispatchResult, "dispatchResult must not be null");

		if (dispatchResult.status() == EvidenceDispatchStatus.REJECTED) {
			throw new IllegalArgumentException(
					EvidenceDispatchExecutionRejectionReason.REJECTED_DISPATCH.name()
			);
		}
		if (dispatchResult.request().routingPlan().paymentConsistencyRequired()
				&& (dispatchResult.request().routingPlan().routes().stream().noneMatch(route ->
						route.scope() == EvidenceQueryRoutingScope.PAYMENT_CONSISTENCY_ROUTE
								&& route.paymentSupporting())
						|| dispatchResult.results().stream().noneMatch(
								EvidenceQueryResult::paymentConsistencyMetadataPresent
						))) {
			throw new IllegalArgumentException(
					EvidenceDispatchExecutionRejectionReason
							.PAYMENT_EVIDENCE_INTEGRITY_REQUIRED.name()
			);
		}
	}

	public boolean dispatchExecutionCandidate() {
		return true;
	}

	public boolean recommendationAuthority() {
		return false;
	}

	public boolean actionExecutionAuthority() {
		return false;
	}

	public boolean exposesRawPayload() {
		return false;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}
}
