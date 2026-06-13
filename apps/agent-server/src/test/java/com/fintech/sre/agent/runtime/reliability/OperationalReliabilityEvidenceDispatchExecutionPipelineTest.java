package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class OperationalReliabilityEvidenceDispatchExecutionPipelineTest {

	@Test
	void shouldRemainReadOnlySemanticPipeline() {
		EvidenceDispatchExecutionPipeline pipeline = pipeline(
				request -> acceptedDispatchResult(request),
				request -> acceptedExecutionResponse(request)
		);

		EvidenceDispatchExecutionPipelineResult result = pipeline.run(
				new EvidenceDispatchExecutionPipelineInput(acceptedDispatchRequest())
		);

		assertThat(pipeline.readOnly()).isTrue();
		assertThat(result.recommendation()).isFalse();
		assertThat(result.executionPermission()).isFalse();
	}

	@Test
	void shouldNotCreateExecutorRequestWhenDispatchRejected() {
		EvidenceDispatchExecutionPipeline pipeline = pipeline(
				request -> rejectedDispatchResult(request),
				request -> {
					throw new AssertionError("executor must not be invoked");
				}
		);

		EvidenceDispatchExecutionPipelineResult result = pipeline.run(
				new EvidenceDispatchExecutionPipelineInput(acceptedDispatchRequest())
		);

		assertThat(result.executionRequest()).isNull();
		assertThat(result.executionResponse()).isNull();
		assertThat(result.rejectionReason())
				.isEqualTo(EvidenceDispatchExecutionPipelineRejectionReason.DISPATCH_REJECTED);
	}

	@Test
	void shouldAllowOnlyNormalizedEvidenceQueryResultsInExecutorResponse() {
		EvidenceDispatchExecutionPipeline pipeline = pipeline(
				request -> acceptedDispatchResult(request),
				request -> acceptedExecutionResponse(request)
		);

		EvidenceDispatchExecutionPipelineResult result = pipeline.run(
				new EvidenceDispatchExecutionPipelineInput(acceptedDispatchRequest())
		);

		assertThat(result.executionResponse().results()).allMatch(
				EvidenceQueryResult::normalizedSemanticEvidenceOnly
		);
	}

	@Test
	void shouldTreatExecutionFailureAsEvidenceUncertaintyNotSystemFailure() {
		EvidenceDispatchExecutionPipeline pipeline = pipeline(
				request -> uncertainDispatchResult(request),
				request -> uncertainExecutionResponse(request)
		);

		EvidenceDispatchExecutionPipelineResult result = pipeline.run(
				new EvidenceDispatchExecutionPipelineInput(acceptedDispatchRequest())
		);

		assertThat(result.propagatedCollectionStatus())
				.isEqualTo(EvidenceCollectionStatus.UNKNOWN);
	}

	@Test
	void shouldPropagatePartialExecutionResultAsPartialCollection() {
		EvidenceDispatchExecutionPipeline pipeline = pipeline(
				request -> acceptedDispatchResult(request),
				request -> response(
						EvidenceDispatchExecutionStatus.ACCEPTED,
						request,
						List.of(result(
								EvidenceSourceType.METRICS,
								EvidenceCollectionStatus.PARTIAL,
								false
						)),
						null
				)
		);

		EvidenceDispatchExecutionPipelineResult result = pipeline.run(
				new EvidenceDispatchExecutionPipelineInput(acceptedDispatchRequest())
		);

		assertThat(result.propagatedCollectionStatus())
				.isEqualTo(EvidenceCollectionStatus.PARTIAL);
	}

	@Test
	void shouldPropagateUnknownExecutionResultAsUnknownCollection() {
		EvidenceDispatchExecutionPipeline pipeline = pipeline(
				request -> uncertainDispatchResult(request),
				request -> uncertainExecutionResponse(request)
		);

		EvidenceDispatchExecutionPipelineResult result = pipeline.run(
				new EvidenceDispatchExecutionPipelineInput(acceptedDispatchRequest())
		);

		assertThat(result.propagatedCollectionStatus())
				.isEqualTo(EvidenceCollectionStatus.UNKNOWN);
	}

	@Test
	void shouldKeepPaymentSafetyUncertaintyWhenPaymentIntegrityIsMissing() {
		EvidenceDispatchExecutionPipeline pipeline = pipeline(
				request -> paymentDispatchResult(request, false),
				request -> acceptedExecutionResponse(request)
		);

		EvidenceDispatchExecutionPipelineResult result = pipeline.run(
				new EvidenceDispatchExecutionPipelineInput(paymentDispatchRequest())
		);

		assertThat(result.paymentSafetyUncertain()).isTrue();
		assertThat(result.rejectionReason()).isEqualTo(
				EvidenceDispatchExecutionPipelineRejectionReason.EXECUTION_REQUEST_REJECTED
		);
	}

	@Test
	void shouldNotExposeRawPayloadOrVendorDetail() {
		EvidenceDispatchExecutionPipeline pipeline = pipeline(
				request -> acceptedDispatchResult(request),
				request -> acceptedExecutionResponse(request)
		);

		EvidenceDispatchExecutionPipelineResult result = pipeline.run(
				new EvidenceDispatchExecutionPipelineInput(acceptedDispatchRequest())
		);

		assertThat(pipeline.exposesRawPayload()).isFalse();
		assertThat(result.exposesRawPayload()).isFalse();
	}

	@Test
	void shouldNotGrantRecommendationOrExecutionPermission() {
		EvidenceDispatchExecutionPipeline pipeline = pipeline(
				request -> acceptedDispatchResult(request),
				request -> acceptedExecutionResponse(request)
		);

		EvidenceDispatchExecutionPipelineResult result = pipeline.run(
				new EvidenceDispatchExecutionPipelineInput(acceptedDispatchRequest())
		);

		assertThat(result.recommendation()).isFalse();
		assertThat(result.executionPermission()).isFalse();
	}

	@Test
	void shouldNotMutatePortfolioKnowledgeSource() {
		EvidenceDispatchExecutionPipeline pipeline = pipeline(
				request -> acceptedDispatchResult(request),
				request -> acceptedExecutionResponse(request)
		);
		EvidenceDispatchExecutionPipelineInput input =
				new EvidenceDispatchExecutionPipelineInput(acceptedDispatchRequest());
		EvidenceDispatchExecutionPipelineResult result = pipeline.run(input);

		assertThat(input.mutatesPortfolioKnowledgeSource()).isFalse();
		assertThat(result.mutatesPortfolioKnowledgeSource()).isFalse();
		assertThat(pipeline.mutatesPortfolioKnowledgeSource()).isFalse();
	}

	@Test
	void shouldNotInvokeExecutorWhenDispatchRejected() {
		AtomicInteger invocations = new AtomicInteger();
		EvidenceDispatchExecutionPipeline pipeline = pipeline(
				request -> rejectedDispatchResult(request),
				request -> {
					invocations.incrementAndGet();
					return acceptedExecutionResponse(request);
				}
		);

		pipeline.run(new EvidenceDispatchExecutionPipelineInput(acceptedDispatchRequest()));

		assertThat(invocations.get()).isZero();
	}

	@Test
	void shouldRejectNullInput() {
		EvidenceDispatchExecutionPipeline pipeline = pipeline(
				request -> acceptedDispatchResult(request),
				request -> acceptedExecutionResponse(request)
		);

		assertThatThrownBy(() -> pipeline.run(null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("input must not be null");
	}

	private EvidenceDispatchExecutionPipeline pipeline(
			EvidenceDispatchContract dispatchContract,
			EvidenceDispatchExecutorPort executorPort
	) {
		return new EvidenceDispatchExecutionPipeline(dispatchContract, executorPort);
	}

	private EvidenceDispatchRequest acceptedDispatchRequest() {
		return new EvidenceDispatchRequest(new EvidenceRoutingPlan(
				EvidenceRoutingPlanStatus.ACCEPTED,
				EvidenceRoutingPlanScope.STANDARD_PLAN,
				List.of(route(EvidenceSourceType.METRICS,
						EvidenceQueryRoutingScope.STANDARD_ROUTE, false)),
				false,
				null
		));
	}

	private EvidenceDispatchRequest paymentDispatchRequest() {
		return new EvidenceDispatchRequest(new EvidenceRoutingPlan(
				EvidenceRoutingPlanStatus.ACCEPTED,
				EvidenceRoutingPlanScope.PAYMENT_CONSISTENCY_PLAN,
				List.of(route(EvidenceSourceType.METRICS,
						EvidenceQueryRoutingScope.PAYMENT_CONSISTENCY_ROUTE, true)),
				true,
				null
		));
	}

	private EvidenceDispatchResult acceptedDispatchResult(
			EvidenceDispatchRequest request
	) {
		return new EvidenceDispatchResult(
				EvidenceDispatchStatus.ACCEPTED,
				request,
				List.of(result(EvidenceSourceType.METRICS, EvidenceCollectionStatus.COLLECTED, false)),
				null
		);
	}

	private EvidenceDispatchResult uncertainDispatchResult(
			EvidenceDispatchRequest request
	) {
		return new EvidenceDispatchResult(
				EvidenceDispatchStatus.UNCERTAIN,
				request,
				List.of(result(EvidenceSourceType.TRACES, EvidenceCollectionStatus.UNKNOWN, false)),
				null
		);
	}

	private EvidenceDispatchResult rejectedDispatchResult(
			EvidenceDispatchRequest request
	) {
		return new EvidenceDispatchResult(
				EvidenceDispatchStatus.REJECTED,
				request,
				List.of(result(EvidenceSourceType.METRICS, EvidenceCollectionStatus.UNKNOWN, false)),
				EvidenceDispatchRejectionReason.REJECTED_ROUTING_PLAN
		);
	}

	private EvidenceDispatchResult paymentDispatchResult(
			EvidenceDispatchRequest request,
			boolean paymentMetadata
	) {
		return new EvidenceDispatchResult(
				EvidenceDispatchStatus.ACCEPTED,
				request,
				List.of(result(EvidenceSourceType.METRICS, EvidenceCollectionStatus.COLLECTED, paymentMetadata)),
				null
		);
	}

	private EvidenceDispatchExecutionResponse acceptedExecutionResponse(
			EvidenceDispatchExecutionRequest request
	) {
		return response(
				EvidenceDispatchExecutionStatus.ACCEPTED,
				request,
				List.of(result(EvidenceSourceType.METRICS, EvidenceCollectionStatus.COLLECTED, false)),
				null
		);
	}

	private EvidenceDispatchExecutionResponse uncertainExecutionResponse(
			EvidenceDispatchExecutionRequest request
	) {
		return response(
				EvidenceDispatchExecutionStatus.UNCERTAIN,
				request,
				List.of(result(EvidenceSourceType.TRACES, EvidenceCollectionStatus.UNKNOWN, false)),
				null
		);
	}

	private EvidenceDispatchExecutionResponse response(
			EvidenceDispatchExecutionStatus status,
			EvidenceDispatchExecutionRequest request,
			List<EvidenceQueryResult> results,
			EvidenceDispatchExecutionRejectionReason rejectionReason
	) {
		return new EvidenceDispatchExecutionResponse(
				status,
				request,
				results,
				rejectionReason
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
						query -> result(query.sourceType(), EvidenceCollectionStatus.UNKNOWN, false)
				)),
				paymentSupporting,
				null
		);
	}

	private EvidenceQueryResult result(
			EvidenceSourceType sourceType,
			EvidenceCollectionStatus status,
			boolean paymentMetadata
	) {
		return new EvidenceQueryResult(
				sourceType,
				status,
				List.of(),
				paymentMetadata
		);
	}
}
