package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

class OperationalReliabilityEvidenceDispatchExecutorPortTest {

	@Test
	void shouldRemainExecutorPortContractOnly() {
		assertThat(EvidenceDispatchExecutorPort.class.isInterface()).isTrue();
	}

	@Test
	void shouldRejectMissingDispatchRequest() {
		assertThatThrownBy(() -> new EvidenceDispatchExecutionRequest(null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("dispatchResult must not be null");
	}

	@Test
	void shouldRejectRejectedDispatchForExecutionRequestCreation() {
		assertThatThrownBy(() -> new EvidenceDispatchExecutionRequest(rejectedDispatch()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage(
						EvidenceDispatchExecutionRejectionReason.REJECTED_DISPATCH.name()
				);
	}

	@Test
	void shouldAllowAcceptedDispatchForExecutionRequestCreation() {
		EvidenceDispatchExecutionRequest request =
				new EvidenceDispatchExecutionRequest(acceptedDispatch());

		assertThat(request.dispatchExecutionCandidate()).isTrue();
	}

	@Test
	void shouldRequirePaymentEvidenceIntegrityForPaymentConsistencyDispatch() {
		assertThatThrownBy(() -> new EvidenceDispatchExecutionRequest(
				paymentDispatchWithoutPaymentRoute()
		))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage(
						EvidenceDispatchExecutionRejectionReason
								.PAYMENT_EVIDENCE_INTEGRITY_REQUIRED.name()
				);
	}

	@Test
	void shouldAllowOnlyNormalizedEvidenceResultsInExecutionResponse() {
		EvidenceDispatchExecutionResponse response =
				new EvidenceDispatchExecutionResponse(
						EvidenceDispatchExecutionStatus.ACCEPTED,
						new EvidenceDispatchExecutionRequest(acceptedDispatch()),
						List.of(unknownResult(EvidenceSourceType.METRICS)),
						null
				);

		assertThat(response.results()).allMatch(
				EvidenceQueryResult::normalizedSemanticEvidenceOnly
		);
	}

	@Test
	void shouldNotExposeRawPayload() {
		EvidenceDispatchExecutionResponse response =
				new EvidenceDispatchExecutionResponse(
						EvidenceDispatchExecutionStatus.RESTRICTED,
						new EvidenceDispatchExecutionRequest(restrictedDispatch()),
						List.of(unknownResult(EvidenceSourceType.LOGS)),
						null
				);

		assertThat(response.exposesRawPayload()).isFalse();
	}

	@Test
	void shouldTreatAdapterExecutionFailureAsEvidenceUncertaintyNotSystemFailure() {
		EvidenceDispatchExecutionResponse response =
				new EvidenceDispatchExecutionResponse(
						EvidenceDispatchExecutionStatus.UNCERTAIN,
						new EvidenceDispatchExecutionRequest(uncertainDispatch()),
						List.of(unknownResult(EvidenceSourceType.TRACES)),
						null
				);

		assertThat(response.uncertaintyOnly()).isTrue();
		assertThat(response.systemFailure()).isFalse();
	}

	@Test
	void shouldNotGrantRecommendationOrActionExecutionAuthority() {
		EvidenceDispatchExecutorPort port = request ->
				new EvidenceDispatchExecutionResponse(
						EvidenceDispatchExecutionStatus.ACCEPTED,
						request,
						List.of(unknownResult(EvidenceSourceType.METRICS)),
						null
				);
		EvidenceDispatchExecutionResponse response = port.execute(
				new EvidenceDispatchExecutionRequest(acceptedDispatch())
		);

		assertThat(port.recommendationAuthority()).isFalse();
		assertThat(port.actionExecutionAuthority()).isFalse();
		assertThat(response.recommendationAuthority()).isFalse();
		assertThat(response.actionExecutionAuthority()).isFalse();
	}

	@Test
	void shouldNotMutatePortfolioKnowledgeSource() {
		EvidenceDispatchExecutorPort port = request ->
				new EvidenceDispatchExecutionResponse(
						EvidenceDispatchExecutionStatus.ACCEPTED,
						request,
						List.of(unknownResult(EvidenceSourceType.LOGS)),
						null
				);
		EvidenceDispatchExecutionRequest request =
				new EvidenceDispatchExecutionRequest(acceptedDispatch());
		EvidenceDispatchExecutionResponse response = port.execute(request);

		assertThat(port.mutatesPortfolioKnowledgeSource()).isFalse();
		assertThat(request.mutatesPortfolioKnowledgeSource()).isFalse();
		assertThat(response.mutatesPortfolioKnowledgeSource()).isFalse();
	}

	@Test
	void shouldExposeSupportedExecutionStatusesAndRejectionReasons() {
		assertThat(EvidenceDispatchExecutionStatus.values()).containsExactly(
				EvidenceDispatchExecutionStatus.ACCEPTED,
				EvidenceDispatchExecutionStatus.RESTRICTED,
				EvidenceDispatchExecutionStatus.UNCERTAIN,
				EvidenceDispatchExecutionStatus.REJECTED
		);
		assertThat(EvidenceDispatchExecutionRejectionReason.values()).containsExactly(
				EvidenceDispatchExecutionRejectionReason.DISPATCH_REQUEST_REQUIRED,
				EvidenceDispatchExecutionRejectionReason.REJECTED_DISPATCH,
				EvidenceDispatchExecutionRejectionReason.PAYMENT_EVIDENCE_INTEGRITY_REQUIRED,
				EvidenceDispatchExecutionRejectionReason.NON_NORMALIZED_RESULT,
				EvidenceDispatchExecutionRejectionReason.UNKNOWN
		);
	}

	private EvidenceDispatchResult acceptedDispatch() {
		return new EvidenceDispatchResult(
				EvidenceDispatchStatus.ACCEPTED,
				new EvidenceDispatchRequest(
						new EvidenceRoutingPlan(
								EvidenceRoutingPlanStatus.ACCEPTED,
								EvidenceRoutingPlanScope.STANDARD_PLAN,
								List.of(route(
										EvidenceSourceType.METRICS,
										EvidenceQueryRoutingScope.STANDARD_ROUTE,
										false
								)),
								false,
								null
						)
				),
				List.of(unknownResult(EvidenceSourceType.METRICS)),
				null
		);
	}

	private EvidenceDispatchResult restrictedDispatch() {
		return new EvidenceDispatchResult(
				EvidenceDispatchStatus.RESTRICTED,
				new EvidenceDispatchRequest(
						new EvidenceRoutingPlan(
								EvidenceRoutingPlanStatus.RESTRICTED,
								EvidenceRoutingPlanScope.RESTRICTED_PLAN,
								List.of(route(
										EvidenceSourceType.LOGS,
										EvidenceQueryRoutingScope.RESTRICTED_ROUTE,
										false
								)),
								false,
								null
						)
				),
				List.of(unknownResult(EvidenceSourceType.LOGS)),
				null
		);
	}

	private EvidenceDispatchResult uncertainDispatch() {
		return new EvidenceDispatchResult(
				EvidenceDispatchStatus.UNCERTAIN,
				new EvidenceDispatchRequest(
						new EvidenceRoutingPlan(
								EvidenceRoutingPlanStatus.UNCERTAIN,
								EvidenceRoutingPlanScope.UNCERTAIN_PLAN,
								List.of(route(
										EvidenceSourceType.TRACES,
										EvidenceQueryRoutingScope.UNCERTAIN_ROUTE,
										false
								)),
								false,
								null
						)
				),
				List.of(unknownResult(EvidenceSourceType.TRACES)),
				null
		);
	}

	private EvidenceDispatchResult rejectedDispatch() {
		return new EvidenceDispatchResult(
				EvidenceDispatchStatus.REJECTED,
				new EvidenceDispatchRequest(
						new EvidenceRoutingPlan(
								EvidenceRoutingPlanStatus.ACCEPTED,
								EvidenceRoutingPlanScope.STANDARD_PLAN,
								List.of(route(
										EvidenceSourceType.METRICS,
										EvidenceQueryRoutingScope.STANDARD_ROUTE,
										false
								)),
								false,
								null
						)
				),
				List.of(unknownResult(EvidenceSourceType.METRICS)),
				EvidenceDispatchRejectionReason.REJECTED_ROUTING_PLAN
		);
	}

	private EvidenceDispatchResult paymentDispatchWithoutPaymentRoute() {
		return new EvidenceDispatchResult(
				EvidenceDispatchStatus.ACCEPTED,
				new EvidenceDispatchRequest(
						new EvidenceRoutingPlan(
								EvidenceRoutingPlanStatus.ACCEPTED,
								EvidenceRoutingPlanScope.PAYMENT_CONSISTENCY_PLAN,
								List.of(route(
										EvidenceSourceType.METRICS,
										EvidenceQueryRoutingScope.PAYMENT_CONSISTENCY_ROUTE,
										true
								)),
								true,
								null
						)
				),
				List.of(unknownResult(EvidenceSourceType.METRICS)),
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
