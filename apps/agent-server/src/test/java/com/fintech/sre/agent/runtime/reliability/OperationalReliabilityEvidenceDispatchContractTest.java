package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

class OperationalReliabilityEvidenceDispatchContractTest {

	@Test
	void shouldRemainDispatchContractOnly() {
		assertThat(EvidenceDispatchContract.class.isInterface()).isTrue();
	}

	@Test
	void shouldAllowAcceptedRestrictedAndUncertainRoutingPlansAsDispatchCandidates() {
		assertThat(new EvidenceDispatchRequest(acceptedPlan()).dispatchCandidate()).isTrue();
		assertThat(new EvidenceDispatchRequest(restrictedPlan()).dispatchCandidate()).isTrue();
		assertThat(new EvidenceDispatchRequest(uncertainPlan()).dispatchCandidate()).isTrue();
	}

	@Test
	void shouldRejectRejectedRoutingPlanForDispatch() {
		assertThatThrownBy(() -> new EvidenceDispatchRequest(rejectedPlan()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage(EvidenceDispatchRejectionReason.REJECTED_ROUTING_PLAN.name());
	}

	@Test
	void shouldRequirePaymentRouteForPaymentConsistencyDispatch() {
		assertThatThrownBy(() -> new EvidenceDispatchRequest(paymentRequiredWithoutRoute()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage(EvidenceDispatchRejectionReason.PAYMENT_ROUTE_REQUIRED.name());
	}

	@Test
	void shouldForbidRawCredentialOrConfigurationInDispatchRequest() {
		EvidenceDispatchRequest request = new EvidenceDispatchRequest(acceptedPlan());

		assertThat(request.exposesRawCredentialOrConfiguration()).isFalse();
	}

	@Test
	void shouldAllowOnlyNormalizedEvidenceQueryResultsInDispatchResult() {
		EvidenceDispatchResult result = new EvidenceDispatchResult(
				EvidenceDispatchStatus.ACCEPTED,
				new EvidenceDispatchRequest(acceptedPlan()),
				List.of(unknownResult(EvidenceSourceType.METRICS)),
				null
		);

		assertThat(result.results()).allMatch(
				EvidenceQueryResult::normalizedSemanticEvidenceOnly
		);
	}

	@Test
	void shouldTreatDispatchFailureAsEvidenceUncertaintyNotSystemFailure() {
		EvidenceDispatchResult result = new EvidenceDispatchResult(
				EvidenceDispatchStatus.UNCERTAIN,
				new EvidenceDispatchRequest(uncertainPlan()),
				List.of(unknownResult(EvidenceSourceType.TRACES)),
				null
		);

		assertThat(result.uncertaintyOnly()).isTrue();
		assertThat(result.systemFailure()).isFalse();
	}

	@Test
	void shouldNotGrantRecommendationOrExecutionAuthority() {
		EvidenceDispatchContract contract = request -> new EvidenceDispatchResult(
				EvidenceDispatchStatus.ACCEPTED,
				request,
				List.of(unknownResult(EvidenceSourceType.LOGS)),
				null
		);

		EvidenceDispatchResult result = contract.dispatch(
				new EvidenceDispatchRequest(acceptedPlan())
		);

		assertThat(contract.recommendationAuthority()).isFalse();
		assertThat(contract.executionAuthority()).isFalse();
		assertThat(result.recommendationAuthority()).isFalse();
		assertThat(result.executionAuthority()).isFalse();
	}

	@Test
	void shouldNotMutatePortfolioKnowledgeSource() {
		EvidenceDispatchContract contract = request -> new EvidenceDispatchResult(
				EvidenceDispatchStatus.RESTRICTED,
				request,
				List.of(unknownResult(EvidenceSourceType.METRICS)),
				null
		);
		EvidenceDispatchRequest request = new EvidenceDispatchRequest(restrictedPlan());
		EvidenceDispatchResult result = contract.dispatch(request);

		assertThat(contract.mutatesPortfolioKnowledgeSource()).isFalse();
		assertThat(request.mutatesPortfolioKnowledgeSource()).isFalse();
		assertThat(result.mutatesPortfolioKnowledgeSource()).isFalse();
	}

	@Test
	void shouldExposeSupportedDispatchStatusesAndRejectionReasons() {
		assertThat(EvidenceDispatchStatus.values()).containsExactly(
				EvidenceDispatchStatus.ACCEPTED,
				EvidenceDispatchStatus.RESTRICTED,
				EvidenceDispatchStatus.UNCERTAIN,
				EvidenceDispatchStatus.REJECTED
		);
		assertThat(EvidenceDispatchRejectionReason.values()).containsExactly(
				EvidenceDispatchRejectionReason.REJECTED_ROUTING_PLAN,
				EvidenceDispatchRejectionReason.PAYMENT_ROUTE_REQUIRED,
				EvidenceDispatchRejectionReason.NON_NORMALIZED_RESULT,
				EvidenceDispatchRejectionReason.UNKNOWN
		);
	}

	private EvidenceRoutingPlan acceptedPlan() {
		return new EvidenceRoutingPlan(
				EvidenceRoutingPlanStatus.ACCEPTED,
				EvidenceRoutingPlanScope.STANDARD_PLAN,
				List.of(route(EvidenceSourceType.METRICS, EvidenceQueryRoutingScope.STANDARD_ROUTE, false)),
				false,
				null
		);
	}

	private EvidenceRoutingPlan restrictedPlan() {
		return new EvidenceRoutingPlan(
				EvidenceRoutingPlanStatus.RESTRICTED,
				EvidenceRoutingPlanScope.RESTRICTED_PLAN,
				List.of(route(EvidenceSourceType.LOGS, EvidenceQueryRoutingScope.RESTRICTED_ROUTE, false)),
				false,
				null
		);
	}

	private EvidenceRoutingPlan uncertainPlan() {
		return new EvidenceRoutingPlan(
				EvidenceRoutingPlanStatus.UNCERTAIN,
				EvidenceRoutingPlanScope.UNCERTAIN_PLAN,
				List.of(route(EvidenceSourceType.TRACES, EvidenceQueryRoutingScope.UNCERTAIN_ROUTE, false)),
				false,
				null
		);
	}

	private EvidenceRoutingPlan rejectedPlan() {
		return new EvidenceRoutingPlan(
				EvidenceRoutingPlanStatus.REJECTED,
				EvidenceRoutingPlanScope.REJECTED_PLAN,
				List.of(),
				false,
				EvidenceRoutingPlanRejectionReason.REJECTED_ROUTE_INCLUDED
		);
	}

	private EvidenceRoutingPlan paymentRequiredWithoutRoute() {
		return new EvidenceRoutingPlan(
				EvidenceRoutingPlanStatus.ACCEPTED,
				EvidenceRoutingPlanScope.PAYMENT_CONSISTENCY_PLAN,
				List.of(route(EvidenceSourceType.METRICS, EvidenceQueryRoutingScope.STANDARD_ROUTE, false)),
				true,
				null
		);
	}

	private EvidenceQueryRoute route(
			EvidenceSourceType sourceType,
			EvidenceQueryRoutingScope scope,
			boolean paymentSupporting
	) {
		return new EvidenceQueryRoute(
				sourceType,
				scope,
				List.of(new EvidenceAdapterRegistration(
						new EvidenceAdapterDescriptor(
								sourceType.name().toLowerCase(),
								sourceType.name().toLowerCase(),
								sourceType,
								EvidenceAdapterAvailability.AVAILABLE,
								true,
								paymentSupporting
						),
						query -> unknownResult(query.sourceType())
				)),
				paymentSupporting,
				null
		);
	}

	private EvidenceQueryResult unknownResult(EvidenceSourceType sourceType) {
		return new EvidenceQueryResult(
				sourceType,
				EvidenceCollectionStatus.UNKNOWN,
				List.of(),
				false
		);
	}
}
