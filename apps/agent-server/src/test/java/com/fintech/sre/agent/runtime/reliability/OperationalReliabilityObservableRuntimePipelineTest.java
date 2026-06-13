package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class OperationalReliabilityObservableRuntimePipelineTest {

	private final ObservableReliabilityRuntimePipeline pipeline =
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

	@Test
	void shouldRemainReadOnly() {
		ObservableReliabilityRuntimeResult result = pipeline.run(input(
				List.of(
						adapter(collected(
								EvidenceSourceType.METRICS,
								signal(EvidenceSignalType.METRIC, "metric-1", "metric")
						)),
						adapter(paymentCollected())
				),
				List.of(
						query(EvidenceSourceType.METRICS, false),
						query(EvidenceSourceType.PAYMENT_CONSISTENCY, true)
				),
				verifiedLifecycleAudit()
		));

		assertThat(result.readOnly()).isTrue();
		assertThat(result.recommendation()).isFalse();
		assertThat(result.executionPermission()).isFalse();
	}

	@Test
	void shouldPreserveFixedObservableRuntimeOrder() {
		ObservableReliabilityRuntimeResult result = pipeline.run(input(
				List.of(
						adapter(collected(
								EvidenceSourceType.METRICS,
								signal(EvidenceSignalType.METRIC, "metric-1", "metric")
						)),
						adapter(paymentCollected())
				),
				List.of(
						query(EvidenceSourceType.METRICS, false),
						query(EvidenceSourceType.PAYMENT_CONSISTENCY, true)
				),
				verifiedLifecycleAudit()
		));

		assertThat(result.stages()).containsExactly(
				ObservableReliabilityRuntimeStage.EVIDENCE_COLLECTION,
				ObservableReliabilityRuntimeStage.EVIDENCE_ASSESSMENT,
				ObservableReliabilityRuntimeStage.ASSESSMENT_LIFECYCLE,
				ObservableReliabilityRuntimeStage.LIFECYCLE_SUMMARY_RESOURCE
		);
	}

	@Test
	void shouldAllowOnlyNormalizedEvidenceIntoObservableRuntime() {
		ObservableReliabilityRuntimeResult result = pipeline.run(input(
				List.of(
						adapter(collected(
								EvidenceSourceType.LOGS,
								signal(EvidenceSignalType.LOG, "log-1", "log")
						)),
						adapter(paymentCollected())
				),
				List.of(
						query(EvidenceSourceType.LOGS, false),
						query(EvidenceSourceType.PAYMENT_CONSISTENCY, true)
				),
				verifiedLifecycleAudit()
		));

		assertThat(result.collectionResult().normalizedSignals())
				.extracting(EvidenceSignal::type)
				.contains(EvidenceSignalType.LOG, EvidenceSignalType.PAYMENT_SAFETY);
	}

	@Test
	void shouldNotExposeRawPayloadOrVendorDetail() {
		ObservableReliabilityRuntimeResult result = pipeline.run(input(
				List.of(adapter(failed(EvidenceSourceType.METRICS))),
				List.of(query(EvidenceSourceType.METRICS, false)),
				verifiedLifecycleAudit()
		));

		assertThat(result.exposesRawPayload()).isFalse();
		assertThat(result.summaryResponse().exposesRawEvidencePayload()).isFalse();
	}

	@Test
	void shouldKeepSummaryResponseAsNonRecommendationAndNonExecutionPermission() {
		ObservableReliabilityRuntimeResult result = pipeline.run(input(
				List.of(adapter(failed(EvidenceSourceType.METRICS))),
				List.of(query(EvidenceSourceType.METRICS, false)),
				verifiedLifecycleAudit()
		));

		assertThat(result.summaryResponse().recommendation()).isFalse();
		assertThat(result.summaryResponse().executionPermission()).isFalse();
	}

	@Test
	void shouldPropagatePaymentInconsistencyAsCriticalRisk() {
		ObservableReliabilityRuntimeResult result = pipeline.run(input(
				List.of(
						adapter(collected(
								EvidenceSourceType.METRICS,
								signal(EvidenceSignalType.METRIC, "metric-1", "metric")
						)),
						adapter(paymentContradictoryCollected())
				),
				List.of(
						query(EvidenceSourceType.METRICS, false),
						query(EvidenceSourceType.PAYMENT_CONSISTENCY, true)
				),
				verifiedLifecycleAudit(),
				List.of(
						new RegressionSignal(
								RegressionSignalType.PAYMENT_INCONSISTENCY,
								"payment-inconsistency-1",
								"payment inconsistency"
						)
				)
		));

		assertThat(result.summaryResponse().summary().risk())
				.isEqualTo(OperationalUncertainty.CRITICAL);
	}

	@Test
	void shouldNotTrustSummaryWhenAuditIncomplete() {
		ObservableReliabilityRuntimeResult result = pipeline.run(input(
				List.of(
						adapter(collected(
								EvidenceSourceType.METRICS,
								signal(EvidenceSignalType.METRIC, "metric-1", "metric")
						)),
						adapter(paymentCollected())
				),
				List.of(
						query(EvidenceSourceType.METRICS, false),
						query(EvidenceSourceType.PAYMENT_CONSISTENCY, true)
				),
				incompleteLifecycleAudit()
		));

		assertThat(result.summaryResponse().summary().auditTrusted()).isFalse();
	}

	@Test
	void shouldNotCallExecutor() {
		ObservableReliabilityRuntimeResult result = pipeline.run(input(
				List.of(adapter(failed(EvidenceSourceType.METRICS))),
				List.of(query(EvidenceSourceType.METRICS, false)),
				verifiedLifecycleAudit()
		));

		assertThat(result.assessmentLifecyclePipelineResult()
				.lifecycleSummary()
				.reason()).isEqualTo(ReliabilityLifecycleSummaryReason.EXECUTOR_RESPONSE_UNKNOWN);
	}

	@Test
	void shouldNotMutatePortfolioKnowledgeSource() {
		ObservableReliabilityRuntimeInput input = input(
				List.of(adapter(failed(EvidenceSourceType.METRICS))),
				List.of(query(EvidenceSourceType.METRICS, false)),
				verifiedLifecycleAudit()
		);
		ObservableReliabilityRuntimeResult result = pipeline.run(input);

		assertThat(input.mutatesPortfolioKnowledgeSource()).isFalse();
		assertThat(result.mutatesPortfolioKnowledgeSource()).isFalse();
	}

	@Test
	void shouldRejectNullInput() {
		assertThatThrownBy(() -> pipeline.run(null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("input must not be null");
	}

	private ObservableReliabilityRuntimeInput input(
			List<EvidenceAdapterPort> adapters,
			List<EvidenceQuery> queries,
			LifecycleAuditDecision lifecycleAuditDecision
	) {
		return input(adapters, queries, lifecycleAuditDecision, List.of());
	}

	private ObservableReliabilityRuntimeInput input(
			List<EvidenceAdapterPort> adapters,
			List<EvidenceQuery> queries,
			LifecycleAuditDecision lifecycleAuditDecision,
			List<RegressionSignal> regressionSignals
	) {
		return new ObservableReliabilityRuntimeInput(
				adapters,
				queries,
				RuntimeState.DEGRADED,
				PropagationSignal.CROSS_SERVICE,
				false,
				false,
				new ConvergenceWindow(
						Duration.ofMinutes(5),
						Duration.ofMinutes(5)
				),
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
				lifecycleAuditDecision
		);
	}

	private EvidenceAdapterPort adapter(EvidenceQueryResult result) {
		return query -> result;
	}

	private EvidenceQuery query(
			EvidenceSourceType sourceType,
			boolean paymentRelated
	) {
		return new EvidenceQuery(
				sourceType,
				"incident-1",
				Instant.parse("2026-05-29T00:00:00Z"),
				Instant.parse("2026-05-29T01:00:00Z"),
				paymentRelated
		);
	}

	private EvidenceQueryResult collected(
			EvidenceSourceType sourceType,
			EvidenceSignal signal
	) {
		return new EvidenceQueryResult(
				sourceType,
				EvidenceCollectionStatus.COLLECTED,
				List.of(signal),
				false
		);
	}

	private EvidenceQueryResult paymentCollected() {
		return new EvidenceQueryResult(
				EvidenceSourceType.PAYMENT_CONSISTENCY,
				EvidenceCollectionStatus.COLLECTED,
				List.of(signal(
						EvidenceSignalType.PAYMENT_SAFETY,
						"payment-1",
						"payment"
				)),
				true
		);
	}

	private EvidenceQueryResult paymentContradictoryCollected() {
		return new EvidenceQueryResult(
				EvidenceSourceType.PAYMENT_CONSISTENCY,
				EvidenceCollectionStatus.COLLECTED,
				List.of(
						signal(EvidenceSignalType.PAYMENT_SAFETY, "payment-1", "consistent"),
						signal(EvidenceSignalType.PAYMENT_SAFETY, "payment-1", "inconsistent")
				),
				true
		);
	}

	private EvidenceQueryResult failed(EvidenceSourceType sourceType) {
		return new EvidenceQueryResult(
				sourceType,
				EvidenceCollectionStatus.FAILED,
				List.of(),
				false
		);
	}

	private LifecycleAuditDecision verifiedLifecycleAudit() {
		return new LifecycleAuditDecision(
				LifecycleAuditTrail.empty(),
				LifecycleAuditIntegrity.VERIFIED
		);
	}

	private LifecycleAuditDecision incompleteLifecycleAudit() {
		return new LifecycleAuditDecision(
				LifecycleAuditTrail.empty(),
				LifecycleAuditIntegrity.INCOMPLETE
		);
	}

	private EvidenceSignal signal(
			EvidenceSignalType type,
			String signalId,
			String summary
	) {
		return new EvidenceSignal(type, signalId, summary);
	}
}
