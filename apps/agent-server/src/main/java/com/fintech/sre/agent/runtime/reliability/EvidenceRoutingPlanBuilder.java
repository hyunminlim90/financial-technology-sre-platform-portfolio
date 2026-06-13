package com.fintech.sre.agent.runtime.reliability;

import java.util.List;
import java.util.Objects;

public class EvidenceRoutingPlanBuilder {

	public EvidenceRoutingPlan build(
			List<EvidenceQueryRoute> routes,
			boolean paymentConsistencyRequired
	) {
		Objects.requireNonNull(routes, "routes must not be null");

		if (routes.isEmpty() || routes.stream().noneMatch(EvidenceQueryRoute::accepted)) {
			return rejected(
					List.copyOf(routes),
					paymentConsistencyRequired,
					EvidenceRoutingPlanRejectionReason.NO_ACCEPTED_ROUTE
			);
		}
		if (routes.stream().anyMatch(route -> route.scope()
				== EvidenceQueryRoutingScope.REJECTED_ROUTE)) {
			return rejected(
					List.copyOf(routes),
					paymentConsistencyRequired,
					rejectionReasonForRejectedRoute(routes)
			);
		}
		if (paymentConsistencyRequired
				&& routes.stream().noneMatch(route -> route.scope()
						== EvidenceQueryRoutingScope.PAYMENT_CONSISTENCY_ROUTE
						&& route.paymentSupporting())) {
			return rejected(
					List.copyOf(routes),
					true,
					EvidenceRoutingPlanRejectionReason.PAYMENT_ROUTE_REQUIRED
			);
		}

		EvidenceRoutingPlanStatus status = statusOf(routes);
		return new EvidenceRoutingPlan(
				status,
				scopeOf(status, paymentConsistencyRequired),
				List.copyOf(routes),
				paymentConsistencyRequired,
				null
		);
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

	private EvidenceRoutingPlan rejected(
			List<EvidenceQueryRoute> routes,
			boolean paymentConsistencyRequired,
			EvidenceRoutingPlanRejectionReason rejectionReason
	) {
		return new EvidenceRoutingPlan(
				EvidenceRoutingPlanStatus.REJECTED,
				EvidenceRoutingPlanScope.REJECTED_PLAN,
				routes,
				paymentConsistencyRequired,
				rejectionReason
		);
	}

	private EvidenceRoutingPlanStatus statusOf(
			List<EvidenceQueryRoute> routes
	) {
		if (routes.stream().anyMatch(EvidenceQueryRoute::uncertain)) {
			return EvidenceRoutingPlanStatus.UNCERTAIN;
		}
		if (routes.stream().anyMatch(EvidenceQueryRoute::restricted)) {
			return EvidenceRoutingPlanStatus.RESTRICTED;
		}
		return EvidenceRoutingPlanStatus.ACCEPTED;
	}

	private EvidenceRoutingPlanScope scopeOf(
			EvidenceRoutingPlanStatus status,
			boolean paymentConsistencyRequired
	) {
		if (paymentConsistencyRequired) {
			return EvidenceRoutingPlanScope.PAYMENT_CONSISTENCY_PLAN;
		}
		return switch (status) {
			case ACCEPTED -> EvidenceRoutingPlanScope.STANDARD_PLAN;
			case RESTRICTED -> EvidenceRoutingPlanScope.RESTRICTED_PLAN;
			case UNCERTAIN -> EvidenceRoutingPlanScope.UNCERTAIN_PLAN;
			case REJECTED -> EvidenceRoutingPlanScope.REJECTED_PLAN;
		};
	}

	private EvidenceRoutingPlanRejectionReason rejectionReasonForRejectedRoute(
			List<EvidenceQueryRoute> routes
	) {
		if (routes.stream().anyMatch(route -> route.rejectionReason()
				== EvidenceQueryRoutingRejectionReason.UNAVAILABLE_ADAPTER)) {
			return EvidenceRoutingPlanRejectionReason.UNAVAILABLE_ROUTE_INCLUDED;
		}
		return EvidenceRoutingPlanRejectionReason.REJECTED_ROUTE_INCLUDED;
	}
}
