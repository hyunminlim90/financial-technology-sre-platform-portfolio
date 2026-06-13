package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class OperationalReliabilityEvidenceRoutingPlanTest {

	private final EvidenceRoutingPlanBuilder builder = new EvidenceRoutingPlanBuilder();

	@Test
	void shouldRejectPlanWhenNoAcceptedRouteExists() {
		EvidenceRoutingPlan plan = builder.build(
				List.of(rejectedRoute(
						EvidenceSourceType.METRICS,
						EvidenceQueryRoutingRejectionReason.NO_SELECTION_AVAILABLE
				)),
				false
		);

		assertThat(plan.accepted()).isFalse();
		assertThat(plan.status()).isEqualTo(EvidenceRoutingPlanStatus.REJECTED);
		assertThat(plan.rejectionReason())
				.isEqualTo(EvidenceRoutingPlanRejectionReason.NO_ACCEPTED_ROUTE);
	}

	@Test
	void shouldRequirePaymentRouteWhenPaymentConsistencyIsRequired() {
		EvidenceRoutingPlan plan = builder.build(
				List.of(standardRoute(EvidenceSourceType.METRICS)),
				true
		);

		assertThat(plan.accepted()).isFalse();
		assertThat(plan.rejectionReason())
				.isEqualTo(EvidenceRoutingPlanRejectionReason.PAYMENT_ROUTE_REQUIRED);
	}

	@Test
	void shouldAcceptPaymentPlanWhenPaymentSupportingRouteExists() {
		EvidenceRoutingPlan plan = builder.build(
				List.of(paymentRoute(EvidenceSourceType.METRICS, true)),
				true
		);

		assertThat(plan.accepted()).isTrue();
		assertThat(plan.scope())
				.isEqualTo(EvidenceRoutingPlanScope.PAYMENT_CONSISTENCY_PLAN);
	}

	@Test
	void shouldMarkPlanRestrictedWhenRestrictedRouteIsIncluded() {
		EvidenceRoutingPlan plan = builder.build(
				List.of(restrictedRoute(EvidenceSourceType.LOGS)),
				false
		);

		assertThat(plan.accepted()).isTrue();
		assertThat(plan.status()).isEqualTo(EvidenceRoutingPlanStatus.RESTRICTED);
		assertThat(plan.scope()).isEqualTo(EvidenceRoutingPlanScope.RESTRICTED_PLAN);
	}

	@Test
	void shouldMarkPlanUncertainWhenUncertainRouteIsIncluded() {
		EvidenceRoutingPlan plan = builder.build(
				List.of(uncertainRoute(EvidenceSourceType.TRACES)),
				false
		);

		assertThat(plan.accepted()).isTrue();
		assertThat(plan.status()).isEqualTo(EvidenceRoutingPlanStatus.UNCERTAIN);
		assertThat(plan.scope()).isEqualTo(EvidenceRoutingPlanScope.UNCERTAIN_PLAN);
	}

	@Test
	void shouldRejectPlanWhenRejectedRouteIsIncluded() {
		EvidenceRoutingPlan plan = builder.build(
				List.of(
						standardRoute(EvidenceSourceType.METRICS),
						rejectedRoute(
								EvidenceSourceType.LOGS,
								EvidenceQueryRoutingRejectionReason.NO_SELECTION_AVAILABLE
						)
				),
				false
		);

		assertThat(plan.accepted()).isFalse();
		assertThat(plan.rejectionReason())
				.isEqualTo(EvidenceRoutingPlanRejectionReason.REJECTED_ROUTE_INCLUDED);
	}

	@Test
	void shouldRejectPlanWhenUnavailableRouteIsIncluded() {
		EvidenceRoutingPlan plan = builder.build(
				List.of(rejectedRoute(
						EvidenceSourceType.TRACES,
						EvidenceQueryRoutingRejectionReason.UNAVAILABLE_ADAPTER
				)),
				false
		);

		assertThat(plan.accepted()).isFalse();
		assertThat(plan.rejectionReason())
				.isEqualTo(EvidenceRoutingPlanRejectionReason.NO_ACCEPTED_ROUTE);
	}

	@Test
	void shouldRemainMetadataOnlyWithoutRecommendationOrExecutionAuthority() {
		EvidenceRoutingPlan plan = builder.build(
				List.of(standardRoute(EvidenceSourceType.METRICS)),
				false
		);

		assertThat(plan.recommendationAuthority()).isFalse();
		assertThat(plan.executionAuthority()).isFalse();
		assertThat(builder.recommendationAuthority()).isFalse();
		assertThat(builder.executionAuthority()).isFalse();
	}

	@Test
	void shouldNotExposeRawCredentialOrConfiguration() {
		EvidenceRoutingPlan plan = builder.build(
				List.of(standardRoute(EvidenceSourceType.LOGS)),
				false
		);

		assertThat(plan.exposesRawCredentialOrConfiguration()).isFalse();
		assertThat(builder.exposesRawCredentialOrConfiguration()).isFalse();
	}

	@Test
	void shouldNotMutatePortfolioKnowledgeSource() {
		EvidenceRoutingPlan plan = builder.build(
				List.of(standardRoute(EvidenceSourceType.TRACES)),
				false
		);

		assertThat(plan.mutatesPortfolioKnowledgeSource()).isFalse();
		assertThat(builder.mutatesPortfolioKnowledgeSource()).isFalse();
	}

	@Test
	void shouldExposeSupportedPlanStatusesScopesAndRejectionReasons() {
		assertThat(EvidenceRoutingPlanStatus.values()).containsExactly(
				EvidenceRoutingPlanStatus.ACCEPTED,
				EvidenceRoutingPlanStatus.RESTRICTED,
				EvidenceRoutingPlanStatus.UNCERTAIN,
				EvidenceRoutingPlanStatus.REJECTED
		);
		assertThat(EvidenceRoutingPlanScope.values()).containsExactly(
				EvidenceRoutingPlanScope.STANDARD_PLAN,
				EvidenceRoutingPlanScope.PAYMENT_CONSISTENCY_PLAN,
				EvidenceRoutingPlanScope.RESTRICTED_PLAN,
				EvidenceRoutingPlanScope.UNCERTAIN_PLAN,
				EvidenceRoutingPlanScope.REJECTED_PLAN
		);
		assertThat(EvidenceRoutingPlanRejectionReason.values()).containsExactly(
				EvidenceRoutingPlanRejectionReason.NO_ACCEPTED_ROUTE,
				EvidenceRoutingPlanRejectionReason.PAYMENT_ROUTE_REQUIRED,
				EvidenceRoutingPlanRejectionReason.REJECTED_ROUTE_INCLUDED,
				EvidenceRoutingPlanRejectionReason.UNAVAILABLE_ROUTE_INCLUDED,
				EvidenceRoutingPlanRejectionReason.UNKNOWN
		);
	}

	private EvidenceQueryRoute standardRoute(EvidenceSourceType sourceType) {
		return new EvidenceQueryRoute(
				sourceType,
				EvidenceQueryRoutingScope.STANDARD_ROUTE,
				List.of(registration(sourceType)),
				false,
				null
		);
	}

	private EvidenceQueryRoute paymentRoute(
			EvidenceSourceType sourceType,
			boolean paymentSupporting
	) {
		return new EvidenceQueryRoute(
				sourceType,
				EvidenceQueryRoutingScope.PAYMENT_CONSISTENCY_ROUTE,
				List.of(registration(sourceType)),
				paymentSupporting,
				null
		);
	}

	private EvidenceQueryRoute restrictedRoute(EvidenceSourceType sourceType) {
		return new EvidenceQueryRoute(
				sourceType,
				EvidenceQueryRoutingScope.RESTRICTED_ROUTE,
				List.of(registration(sourceType)),
				false,
				null
		);
	}

	private EvidenceQueryRoute uncertainRoute(EvidenceSourceType sourceType) {
		return new EvidenceQueryRoute(
				sourceType,
				EvidenceQueryRoutingScope.UNCERTAIN_ROUTE,
				List.of(registration(sourceType)),
				false,
				null
		);
	}

	private EvidenceQueryRoute rejectedRoute(
			EvidenceSourceType sourceType,
			EvidenceQueryRoutingRejectionReason rejectionReason
	) {
		return new EvidenceQueryRoute(
				sourceType,
				EvidenceQueryRoutingScope.REJECTED_ROUTE,
				List.of(),
				false,
				rejectionReason
		);
	}

	private EvidenceAdapterRegistration registration(EvidenceSourceType sourceType) {
		return new EvidenceAdapterRegistration(
				new EvidenceAdapterDescriptor(
						sourceType.name().toLowerCase(),
						sourceType.name().toLowerCase(),
						sourceType,
						EvidenceAdapterAvailability.AVAILABLE,
						true,
						sourceType == EvidenceSourceType.METRICS
				),
				query -> new EvidenceQueryResult(
						query.sourceType(),
						EvidenceCollectionStatus.UNKNOWN,
						List.of(),
						false
				)
		);
	}
}
