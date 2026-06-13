package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class OperationalReliabilityEvidenceExecutionObservablePipelineTest {

	private final ObservableReliabilityRuntimePipeline observableRuntimePipeline =
			new ObservableReliabilityRuntimePipeline(
					new EvidenceCollectionOrchestrator(),
					new EvidenceAssessmentPipeline(
							new ReliabilityAssessmentOrchestrator(new VerificationGate())
					),
					new AssessmentLifecyclePipeline(
							new ReliabilityRiskClassifier(),
							new HumanApprovalPolicy(),
							new ReliabilityRecommendationBoundary(),
							new ActionCommandBoundary(),
							new ScenarioBinding(),
							new RollbackVerificationBinding(),
							new SafetyPolicyGate(),
							new ActionAdmissionGate(),
							new ExecutionBoundary(),
							new ReliabilityExecutorContract(),
							new ExecutionReadinessGate(),
							new ReliabilityLifecycleSummaryBuilder()
					),
					new ReliabilityLifecycleSummaryResource()
			);
	private final EvidenceExecutionObservablePipeline pipeline =
			new EvidenceExecutionObservablePipeline(observableRuntimePipeline);

	@Test
	void shouldRemainReadOnlySemanticPipeline() {
		EvidenceExecutionObservablePipelineResult result = pipeline.run(input(
				dispatchExecutionResult(
						EvidenceDispatchExecutionStatus.ACCEPTED,
						List.of(result(
								EvidenceSourceType.METRICS,
								EvidenceCollectionStatus.COLLECTED,
								EvidenceSignalType.METRIC,
								false
						)),
						false
				)
		));

		assertThat(pipeline.readOnly()).isTrue();
		assertThat(result.readOnly()).isTrue();
		assertThat(result.recommendation()).isFalse();
		assertThat(result.executionPermission()).isFalse();
	}

	@Test
	void shouldPreserveFixedExecutionToObservableOrder() {
		EvidenceExecutionObservablePipelineResult result = pipeline.run(input(
				dispatchExecutionResult(
						EvidenceDispatchExecutionStatus.ACCEPTED,
						List.of(result(
								EvidenceSourceType.LOGS,
								EvidenceCollectionStatus.COLLECTED,
								EvidenceSignalType.LOG,
								false
						)),
						false
				)
		));

		assertThat(result.stages()).containsExactly(
				EvidenceExecutionObservablePipelineStage.EVIDENCE_DISPATCH_EXECUTION,
				EvidenceExecutionObservablePipelineStage.OBSERVABLE_RUNTIME
		);
	}

	@Test
	void shouldAllowOnlyNormalizedExecutionEvidenceIntoObservableRuntime() {
		EvidenceExecutionObservablePipelineResult result = pipeline.run(input(
				dispatchExecutionResult(
						EvidenceDispatchExecutionStatus.ACCEPTED,
						List.of(result(
								EvidenceSourceType.LOGS,
								EvidenceCollectionStatus.COLLECTED,
								EvidenceSignalType.LOG,
								false
						)),
						false
				)
		));

		assertThat(result.observableRuntimeResult().collectionResult().normalizedSignals())
				.extracting(EvidenceSignal::type)
				.containsExactly(EvidenceSignalType.LOG);
	}

	@Test
	void shouldPropagateExecutionUnknownAsEvidenceUncertainty() {
		EvidenceExecutionObservablePipelineResult result = pipeline.run(input(
				dispatchExecutionResult(
						EvidenceDispatchExecutionStatus.UNCERTAIN,
						List.of(result(
								EvidenceSourceType.TRACES,
								EvidenceCollectionStatus.UNKNOWN,
								EvidenceSignalType.TRACE,
								false
						)),
						false
				)
		));

		assertThat(result.observableRuntimeResult().collectionResult().status())
				.isEqualTo(EvidenceCollectionStatus.UNKNOWN);
	}

	@Test
	void shouldPropagatePartialExecutionAsPartialCollection() {
		EvidenceExecutionObservablePipelineResult result = pipeline.run(input(
				dispatchExecutionResult(
						EvidenceDispatchExecutionStatus.ACCEPTED,
						List.of(result(
								EvidenceSourceType.METRICS,
								EvidenceCollectionStatus.PARTIAL,
								EvidenceSignalType.METRIC,
								false
						)),
						false
				)
		));

		assertThat(result.observableRuntimeResult().collectionResult().status())
				.isEqualTo(EvidenceCollectionStatus.PARTIAL);
	}

	@Test
	void shouldKeepPaymentSafetyUncertaintyWhenPaymentIntegrityMissing() {
		EvidenceExecutionObservablePipelineResult result = pipeline.run(input(
				dispatchExecutionResult(
						EvidenceDispatchExecutionStatus.ACCEPTED,
						List.of(result(
								EvidenceSourceType.METRICS,
								EvidenceCollectionStatus.UNKNOWN,
								EvidenceSignalType.METRIC,
								false
						)),
						true
				)
		));

		assertThat(result.dispatchExecutionPipelineResult().paymentSafetyUncertain())
				.isTrue();
		assertThat(result.observableRuntimeResult().assessmentPipelineResult()
				.assessmentResult()
				.evidenceCorrelation()
				.paymentSafetyUncertain()).isTrue();
	}

	@Test
	void shouldNotExposeRawPayloadOrVendorDetail() {
		EvidenceExecutionObservablePipelineResult result = pipeline.run(input(
				dispatchExecutionResult(
						EvidenceDispatchExecutionStatus.ACCEPTED,
						List.of(result(
								EvidenceSourceType.LOGS,
								EvidenceCollectionStatus.COLLECTED,
								EvidenceSignalType.LOG,
								false
						)),
						false
				)
		));

		assertThat(pipeline.exposesRawPayload()).isFalse();
		assertThat(result.exposesRawPayload()).isFalse();
		assertThat(result.observableRuntimeResult().summaryResponse()
				.exposesRawEvidencePayload()).isFalse();
	}

	@Test
	void shouldKeepLifecycleSummaryAsNonRecommendationAndNonExecutionPermission() {
		EvidenceExecutionObservablePipelineResult result = pipeline.run(input(
				dispatchExecutionResult(
						EvidenceDispatchExecutionStatus.ACCEPTED,
						List.of(result(
								EvidenceSourceType.METRICS,
								EvidenceCollectionStatus.COLLECTED,
								EvidenceSignalType.METRIC,
								false
						)),
						false
				)
		));

		assertThat(result.observableRuntimeResult().summaryResponse().recommendation())
				.isFalse();
		assertThat(result.observableRuntimeResult().summaryResponse().executionPermission())
				.isFalse();
	}

	@Test
	void shouldNotExecuteActionCommandRollbackOrKubernetes() {
		EvidenceExecutionObservablePipelineResult result = pipeline.run(input(
				dispatchExecutionResult(
						EvidenceDispatchExecutionStatus.ACCEPTED,
						List.of(result(
								EvidenceSourceType.METRICS,
								EvidenceCollectionStatus.COLLECTED,
								EvidenceSignalType.METRIC,
								false
						)),
						false
				)
		));

		assertThat(result.observableRuntimeResult().assessmentLifecyclePipelineResult()
				.lifecycleSummary()
				.reason()).isEqualTo(ReliabilityLifecycleSummaryReason.EXECUTOR_RESPONSE_UNKNOWN);
	}

	@Test
	void shouldNotMutatePortfolioKnowledgeSource() {
		EvidenceExecutionObservablePipelineInput input = input(
				dispatchExecutionResult(
						EvidenceDispatchExecutionStatus.ACCEPTED,
						List.of(result(
								EvidenceSourceType.TRACES,
								EvidenceCollectionStatus.COLLECTED,
								EvidenceSignalType.TRACE,
								false
						)),
						false
				)
		);
		EvidenceExecutionObservablePipelineResult result = pipeline.run(input);

		assertThat(input.mutatesPortfolioKnowledgeSource()).isFalse();
		assertThat(result.mutatesPortfolioKnowledgeSource()).isFalse();
		assertThat(pipeline.mutatesPortfolioKnowledgeSource()).isFalse();
	}

	@Test
	void shouldRejectNullInput() {
		assertThatThrownBy(() -> pipeline.run(null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("input must not be null");
	}

	private EvidenceExecutionObservablePipelineInput input(
			EvidenceDispatchExecutionPipelineResult dispatchExecutionPipelineResult
	) {
		return new EvidenceExecutionObservablePipelineInput(
				dispatchExecutionPipelineResult,
				"incident-1",
				Instant.parse("2026-05-30T00:00:00Z"),
				Instant.parse("2026-05-30T01:00:00Z"),
				RuntimeState.DEGRADED,
				PropagationSignal.CROSS_SERVICE,
				false,
				false,
				new ConvergenceWindow(Duration.ofMinutes(5), Duration.ofMinutes(5)),
				List.of(),
				new ScenarioReference("scenario-1", "portfolio-runtime", true, false),
				new RollbackReference("rollback-1", "portfolio-runtime", true, false),
				new VerificationReference(
						"verification-1",
						"portfolio-runtime",
						true,
						false,
						true
				),
				true,
				true,
				true,
				false,
				true,
				true,
				true,
				true,
				new LifecycleAuditDecision(
						LifecycleAuditTrail.empty(),
						LifecycleAuditIntegrity.VERIFIED
				)
		);
	}

	private EvidenceDispatchExecutionPipelineResult dispatchExecutionResult(
			EvidenceDispatchExecutionStatus status,
			List<EvidenceQueryResult> results,
			boolean paymentRequired
	) {
		EvidenceDispatchRequest dispatchRequest = new EvidenceDispatchRequest(
				new EvidenceRoutingPlan(
						EvidenceRoutingPlanStatus.ACCEPTED,
						paymentRequired
								? EvidenceRoutingPlanScope.PAYMENT_CONSISTENCY_PLAN
								: EvidenceRoutingPlanScope.STANDARD_PLAN,
						List.of(new EvidenceQueryRoute(
								EvidenceSourceType.METRICS,
								paymentRequired
										? EvidenceQueryRoutingScope.PAYMENT_CONSISTENCY_ROUTE
										: EvidenceQueryRoutingScope.STANDARD_ROUTE,
								List.of(new EvidenceAdapterRegistration(
										new EvidenceAdapterDescriptor(
												"adapter-1",
												"adapter-1",
												EvidenceSourceType.METRICS,
												EvidenceAdapterAvailability.AVAILABLE,
												true,
												paymentRequired
										),
										query -> results.get(0)
								)),
								paymentRequired,
								null
						)),
						paymentRequired,
						null
				)
		);
		EvidenceDispatchResult dispatchResult = new EvidenceDispatchResult(
				status == EvidenceDispatchExecutionStatus.REJECTED
						? EvidenceDispatchStatus.REJECTED
						: status == EvidenceDispatchExecutionStatus.UNCERTAIN
								? EvidenceDispatchStatus.UNCERTAIN
								: EvidenceDispatchStatus.ACCEPTED,
				dispatchRequest,
				results,
				status == EvidenceDispatchExecutionStatus.REJECTED
						? EvidenceDispatchRejectionReason.REJECTED_ROUTING_PLAN
						: null
		);

		EvidenceDispatchExecutionRequest executionRequest;
		try {
			executionRequest = new EvidenceDispatchExecutionRequest(dispatchResult);
		} catch (IllegalArgumentException exception) {
			return new EvidenceDispatchExecutionPipelineResult(
					List.of(
							EvidenceDispatchExecutionPipelineStage.DISPATCH,
							EvidenceDispatchExecutionPipelineStage.EXECUTION_REQUEST
					),
					dispatchResult,
					null,
					null,
					EvidenceCollectionStatus.UNKNOWN,
					paymentRequired,
					EvidenceDispatchExecutionPipelineRejectionReason.EXECUTION_REQUEST_REJECTED
			);
		}

		EvidenceDispatchExecutionResponse response = new EvidenceDispatchExecutionResponse(
				status,
				executionRequest,
				results,
				status == EvidenceDispatchExecutionStatus.REJECTED
						? EvidenceDispatchExecutionRejectionReason.REJECTED_DISPATCH
						: null
		);

		return new EvidenceDispatchExecutionPipelineResult(
				List.of(
						EvidenceDispatchExecutionPipelineStage.DISPATCH,
						EvidenceDispatchExecutionPipelineStage.EXECUTION_REQUEST,
						EvidenceDispatchExecutionPipelineStage.EXECUTION_RESPONSE
				),
				dispatchResult,
				executionRequest,
				response,
				results.stream().anyMatch(result -> result.status() == EvidenceCollectionStatus.PARTIAL)
						? EvidenceCollectionStatus.PARTIAL
						: results.stream().anyMatch(result -> result.status() == EvidenceCollectionStatus.UNKNOWN)
								? EvidenceCollectionStatus.UNKNOWN
								: EvidenceCollectionStatus.COLLECTED,
				paymentRequired && results.stream().noneMatch(EvidenceQueryResult::paymentConsistencyMetadataPresent),
				status == EvidenceDispatchExecutionStatus.REJECTED
						? EvidenceDispatchExecutionPipelineRejectionReason.EXECUTION_RESPONSE_REJECTED
						: null
		);
	}

	private EvidenceQueryResult result(
			EvidenceSourceType sourceType,
			EvidenceCollectionStatus status,
			EvidenceSignalType signalType,
			boolean paymentConsistencyMetadataPresent
	) {
		return new EvidenceQueryResult(
				sourceType,
				status,
				List.of(new EvidenceSignal(signalType, "signal-1", "summary-1")),
				paymentConsistencyMetadataPresent
		);
	}
}
