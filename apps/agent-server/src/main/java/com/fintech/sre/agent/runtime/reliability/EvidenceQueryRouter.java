package com.fintech.sre.agent.runtime.reliability;

import java.util.List;
import java.util.Objects;

public class EvidenceQueryRouter {

	public EvidenceQueryRoutingDecision route(
			EvidenceAdapterSelection selection
	) {
		Objects.requireNonNull(selection, "selection must not be null");

		EvidenceQueryRoute route = selection.selected()
				? acceptedRoute(selection)
				: rejectedRoute(selection);
		return new EvidenceQueryRoutingDecision(selection, route);
	}

	public boolean discoveryOnly() {
		return true;
	}

	public boolean executesEvidenceQuery() {
		return false;
	}

	public boolean recommendationAuthority() {
		return false;
	}

	public boolean executionAuthority() {
		return false;
	}

	public boolean systemFailure() {
		return false;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}

	private EvidenceQueryRoute acceptedRoute(
			EvidenceAdapterSelection selection
	) {
		return new EvidenceQueryRoute(
				selection.sourceType(),
				scopeOf(selection),
				selection.registrations(),
				selection.registrations().stream().allMatch(registration ->
						selection.scope() != EvidenceAdapterSelectionScope
								.PAYMENT_CONSISTENCY_EVIDENCE
								|| registration.descriptor().supportsPaymentEvidence()),
				null
		);
	}

	private EvidenceQueryRoute rejectedRoute(
			EvidenceAdapterSelection selection
	) {
		return new EvidenceQueryRoute(
				selection.sourceType(),
				EvidenceQueryRoutingScope.REJECTED_ROUTE,
				List.of(),
				false,
				rejectionReasonOf(selection)
		);
	}

	private EvidenceQueryRoutingScope scopeOf(
			EvidenceAdapterSelection selection
	) {
		return switch (selection.scope()) {
			case STANDARD_EVIDENCE -> EvidenceQueryRoutingScope.STANDARD_ROUTE;
			case PAYMENT_CONSISTENCY_EVIDENCE ->
					EvidenceQueryRoutingScope.PAYMENT_CONSISTENCY_ROUTE;
			case RESTRICTED_FALLBACK -> EvidenceQueryRoutingScope.RESTRICTED_ROUTE;
			case UNCERTAIN_FALLBACK -> EvidenceQueryRoutingScope.UNCERTAIN_ROUTE;
		};
	}

	private EvidenceQueryRoutingRejectionReason rejectionReasonOf(
			EvidenceAdapterSelection selection
	) {
		if (selection.rejectionReason()
				== EvidenceAdapterSelectionRejectionReason
						.PAYMENT_EVIDENCE_NOT_SUPPORTED) {
			return EvidenceQueryRoutingRejectionReason.PAYMENT_SUPPORT_REQUIRED;
		}
		if (selection.rejectionReason()
				== EvidenceAdapterSelectionRejectionReason.NO_AVAILABLE_ADAPTER) {
			return EvidenceQueryRoutingRejectionReason.UNAVAILABLE_ADAPTER;
		}
		if (selection.rejectionReason()
				== EvidenceAdapterSelectionRejectionReason.NO_REGISTERED_ADAPTER) {
			return EvidenceQueryRoutingRejectionReason.NO_SELECTION_AVAILABLE;
		}
		return EvidenceQueryRoutingRejectionReason.UNKNOWN;
	}
}
