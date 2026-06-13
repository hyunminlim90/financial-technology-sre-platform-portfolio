package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class OperationalReliabilityEvidenceRuntimeReadModelTest {

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
	private final EvidenceRuntimeSummaryBuilder builder =
			new EvidenceRuntimeSummaryBuilder();

	@Test
	void shouldExposeReadOnlyOperatorFacingSummaryOnly() {
		EvidenceRuntimeSummary summary = builder.build(result(
				EvidenceDispatchExecutionStatus.ACCEPTED,
				List.of(queryResult(
						EvidenceSourceType.METRICS,
						EvidenceCollectionStatus.COLLECTED,
						EvidenceSignalType.METRIC,
						false
				)),
				false
		));

		assertThat(summary.readOnly()).isTrue();
		assertThat(summary.recommendation()).isFalse();
		assertThat(summary.executionPermission()).isFalse();
		assertThat(summary.actionAdmissionResult()).isFalse();
		assertThat(summary.mutatesPortfolioKnowledgeSource()).isFalse();
		assertThat(summary.view().operatorFacingOnly()).isTrue();
	}

	@Test
	void shouldPromotePaymentInconsistencyToCriticalRisk() {
		EvidenceRuntimeSummary summary = builder.build(result(
				EvidenceDispatchExecutionStatus.ACCEPTED,
				List.of(
						queryResult(
								EvidenceSourceType.METRICS,
								EvidenceCollectionStatus.COLLECTED,
								EvidenceSignalType.METRIC,
								false
						),
						queryResult(
								EvidenceSourceType.PAYMENT_CONSISTENCY,
								EvidenceCollectionStatus.COLLECTED,
								EvidenceSignalType.PAYMENT_SAFETY,
								true
						)
				),
				true,
				List.of(new RegressionSignal(
						RegressionSignalType.PAYMENT_INCONSISTENCY,
						"payment-inconsistency-1",
						"payment inconsistency"
				))
		));

		EvidenceRuntimeSummaryView view = builder.view(summary);

		assertThat(summary.summaryStatus()).isEqualTo(EvidenceRuntimeSummaryStatus.DEGRADED);
		assertThat(summary.riskLevel()).isEqualTo(OperationalUncertainty.CRITICAL);
		assertThat(summary.uncertaintyReason())
				.isEqualTo(EvidenceRuntimeSummaryReason.PAYMENT_INCONSISTENCY);
		assertThat(view.riskLevel()).isEqualTo(OperationalUncertainty.CRITICAL);
	}

	@Test
	void shouldKeepPaymentIntegrityMissingAsPaymentSafetyUncertainty() {
		EvidenceRuntimeSummary summary = builder.build(result(
				EvidenceDispatchExecutionStatus.ACCEPTED,
				List.of(queryResult(
						EvidenceSourceType.METRICS,
						EvidenceCollectionStatus.UNKNOWN,
						EvidenceSignalType.METRIC,
						false
				)),
				true,
				List.of()
		));

		assertThat(summary.summaryStatus()).isEqualTo(EvidenceRuntimeSummaryStatus.UNCERTAIN);
		assertThat(summary.paymentSafetyState().requiresHumanEscalation()).isTrue();
		assertThat(summary.uncertaintyReason())
				.isEqualTo(EvidenceRuntimeSummaryReason.PAYMENT_SAFETY_UNCERTAINTY);
	}

	@Test
	void shouldTreatAdapterFailureAsEvidenceUncertaintyInsteadOfSystemFailure() {
		EvidenceRuntimeSummary summary = builder.build(result(
				EvidenceDispatchExecutionStatus.ACCEPTED,
				List.of(queryResult(
						EvidenceSourceType.METRICS,
						EvidenceCollectionStatus.FAILED,
						EvidenceSignalType.METRIC,
						false
				)),
				false,
				List.of()
		));

		assertThat(summary.summaryStatus()).isEqualTo(EvidenceRuntimeSummaryStatus.UNCERTAIN);
		assertThat(summary.uncertaintyReason())
				.isEqualTo(EvidenceRuntimeSummaryReason.ADAPTER_FAILURE);
	}

	@Test
	void shouldNotExposeRawPayloadVendorDetailOrCredentials() {
		EvidenceRuntimeSummary summary = builder.build(result(
				EvidenceDispatchExecutionStatus.UNCERTAIN,
				List.of(queryResult(
						EvidenceSourceType.LOGS,
						EvidenceCollectionStatus.UNKNOWN,
						EvidenceSignalType.LOG,
						false
				)),
				false,
				List.of()
		));
		EvidenceRuntimeSummaryView view = builder.view(summary);

		assertThat(summary.exposesRawPayload()).isFalse();
		assertThat(summary.exposesVendorDetail()).isFalse();
		assertThat(summary.exposesCredentialConfiguration()).isFalse();
		assertThat(view.exposesRawPayload()).isFalse();
		assertThat(view.exposesVendorDetail()).isFalse();
		assertThat(view.exposesCredentialConfiguration()).isFalse();
	}

	@Test
	void shouldExposeOperatorFacingViewFieldsOnly() {
		EvidenceRuntimeSummary summary = builder.build(result(
				EvidenceDispatchExecutionStatus.ACCEPTED,
				List.of(
						queryResult(
								EvidenceSourceType.TRACES,
								EvidenceCollectionStatus.PARTIAL,
								EvidenceSignalType.TRACE,
								false
						),
						queryResult(
								EvidenceSourceType.PAYMENT_CONSISTENCY,
								EvidenceCollectionStatus.COLLECTED,
								EvidenceSignalType.PAYMENT_SAFETY,
								true
						)
				),
				false,
				List.of()
		));
		EvidenceRuntimeSummaryView view = builder.view(summary);

		assertThat(view.summaryStatus()).isEqualTo(EvidenceRuntimeSummaryStatus.PARTIAL);
		assertThat(view.evidenceCompleteness()).isEqualTo(EvidenceCompleteness.PARTIAL);
		assertThat(view.auditTrusted()).isTrue();
		assertThat(view.uncertaintyDetected()).isTrue();
	}

	@Test
	void shouldRejectNullPipelineResult() {
		assertThatThrownBy(() -> builder.build(null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("pipelineResult must not be null");
	}

	private EvidenceExecutionObservablePipelineResult result(
			EvidenceDispatchExecutionStatus status,
			List<EvidenceQueryResult> queryResults,
			boolean paymentRequired
	) {
		return result(status, queryResults, paymentRequired, List.of());
	}

	private EvidenceExecutionObservablePipelineResult result(
			EvidenceDispatchExecutionStatus status,
			List<EvidenceQueryResult> queryResults,
			boolean paymentRequired,
			List<RegressionSignal> regressionSignals
	) {
		return pipeline.run(new EvidenceExecutionObservablePipelineInput(
				dispatchExecutionResult(status, queryResults, paymentRequired),
				"incident-1",
				Instant.parse("2026-05-30T00:00:00Z"),
				Instant.parse("2026-05-30T01:00:00Z"),
				RuntimeState.DEGRADED,
				PropagationSignal.CROSS_SERVICE,
				false,
				false,
				new ConvergenceWindow(Duration.ofMinutes(5), Duration.ofMinutes(5)),
				regressionSignals,
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
		));
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
								paymentRequired
										? EvidenceSourceType.PAYMENT_CONSISTENCY
										: results.get(0).sourceType(),
								paymentRequired
										? EvidenceQueryRoutingScope.PAYMENT_CONSISTENCY_ROUTE
										: EvidenceQueryRoutingScope.STANDARD_ROUTE,
								List.of(new EvidenceAdapterRegistration(
										new EvidenceAdapterDescriptor(
												"adapter-1",
												"adapter-1",
												paymentRequired
														? EvidenceSourceType.PAYMENT_CONSISTENCY
														: results.get(0).sourceType(),
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
				propagatedCollectionStatus(results),
				paymentRequired && results.stream().noneMatch(EvidenceQueryResult::paymentConsistencyMetadataPresent),
				status == EvidenceDispatchExecutionStatus.REJECTED
						? EvidenceDispatchExecutionPipelineRejectionReason.EXECUTION_RESPONSE_REJECTED
						: null
		);
	}

	private EvidenceCollectionStatus propagatedCollectionStatus(
			List<EvidenceQueryResult> results
	) {
		if (results.stream().anyMatch(result -> result.status() == EvidenceCollectionStatus.FAILED)) {
			return EvidenceCollectionStatus.FAILED;
		}
		if (results.stream().anyMatch(result -> result.status() == EvidenceCollectionStatus.PARTIAL)) {
			return EvidenceCollectionStatus.PARTIAL;
		}
		if (results.stream().anyMatch(result -> result.status() == EvidenceCollectionStatus.UNKNOWN)) {
			return EvidenceCollectionStatus.UNKNOWN;
		}
		if (results.stream().allMatch(result -> result.status() == EvidenceCollectionStatus.ABSENT)) {
			return EvidenceCollectionStatus.ABSENT;
		}
		return EvidenceCollectionStatus.COLLECTED;
	}

	private EvidenceQueryResult queryResult(
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
