package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class OperationalReliabilityAssessmentLifecyclePipelineTest {

	private final AssessmentLifecyclePipeline pipeline =
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
			);
	private final EvidenceAssessmentPipeline evidenceAssessmentPipeline =
			new EvidenceAssessmentPipeline(
					new ReliabilityAssessmentOrchestrator(new VerificationGate())
			);

	@Test
	void shouldRemainReadOnlySemanticPipeline() {
		AssessmentLifecyclePipelineResult result = pipeline.run(input(
				assessmentPipelineResult(completeCollectedResult(), RuntimeState.DEGRADED),
				knownScenario(),
				knownRollback(),
				knownVerification(true),
				verifiedLifecycleAudit()
		));

		assertThat(result.recommendation()).isFalse();
		assertThat(result.executionPermission()).isFalse();
		assertThat(result.exposesRawPayload()).isFalse();
	}

	@Test
	void shouldPreserveFixedAssessmentToLifecycleOrder() {
		AssessmentLifecyclePipelineResult result = pipeline.run(input(
				assessmentPipelineResult(completeCollectedResult(), RuntimeState.DEGRADED),
				knownScenario(),
				knownRollback(),
				knownVerification(true),
				verifiedLifecycleAudit()
		));

		assertThat(result.stages()).containsExactly(
				AssessmentLifecyclePipelineStage.RISK_CLASSIFICATION,
				AssessmentLifecyclePipelineStage.HUMAN_APPROVAL_POLICY,
				AssessmentLifecyclePipelineStage.RECOMMENDATION_BOUNDARY,
				AssessmentLifecyclePipelineStage.ACTION_COMMAND_BOUNDARY,
				AssessmentLifecyclePipelineStage.SCENARIO_BINDING,
				AssessmentLifecyclePipelineStage.ROLLBACK_VERIFICATION_BINDING,
				AssessmentLifecyclePipelineStage.SAFETY_POLICY_GATE,
				AssessmentLifecyclePipelineStage.ACTION_ADMISSION,
				AssessmentLifecyclePipelineStage.EXECUTION_READINESS,
				AssessmentLifecyclePipelineStage.LIFECYCLE_SUMMARY
		);
	}

	@Test
	void shouldKeepAssessmentResultAsNonRecommendationAndNonExecutionPermission() {
		AssessmentLifecyclePipelineResult result = pipeline.run(input(
				assessmentPipelineResult(completeCollectedResult(), RuntimeState.DEGRADED),
				knownScenario(),
				knownRollback(),
				knownVerification(true),
				verifiedLifecycleAudit()
		));

		assertThat(result.assessmentPipelineResult().assessmentResult().semanticOnly())
				.isTrue();
		assertThat(result.assessmentPipelineResult().assessmentResult().executionTrigger())
				.isFalse();
	}

	@Test
	void shouldPropagateRiskClassificationIntoLifecycleSummaryRisk() {
		AssessmentLifecyclePipelineResult result = pipeline.run(input(
				assessmentPipelineResult(completeCollectedResult(), RuntimeState.CONVERGED),
				knownScenario(),
				knownRollback(),
				knownVerification(true),
				verifiedLifecycleAudit()
		));

		assertThat(result.lifecycleSummary().risk().ordinal())
				.isGreaterThanOrEqualTo(map(result.riskClassification().level()).ordinal());
	}

	@Test
	void shouldPropagatePaymentUncertaintyIntoLifecycleRisk() {
		AssessmentLifecyclePipelineResult result = pipeline.run(input(
				assessmentPipelineResult(collectedWithoutPaymentSafety(), RuntimeState.DEGRADED),
				knownScenario(),
				knownRollback(),
				knownVerification(false),
				verifiedLifecycleAudit()
		));

		assertThat(result.assessmentPipelineResult().assessmentResult()
				.evidenceCorrelation()
				.paymentSafetyUncertain()).isTrue();
		assertThat(result.lifecycleSummary().risk()).isEqualTo(OperationalUncertainty.CRITICAL);
	}

	@Test
	void shouldPropagateContradictoryEvidenceIntoLifecycleUncertaintyRegressionCandidate() {
		AssessmentLifecyclePipelineResult result = pipeline.run(input(
				assessmentPipelineResult(contradictoryCollectedResult(), RuntimeState.DEGRADED),
				knownScenario(),
				knownRollback(),
				knownVerification(true),
				verifiedLifecycleAudit()
		));

		assertThat(result.assessmentPipelineResult().assessmentResult()
				.evidenceCorrelation()
				.contradictoryEvidence()).isTrue();
		assertThat(result.lifecycleSummary().reason()).isEqualTo(
				ReliabilityLifecycleSummaryReason.REGRESSION_DETECTED
		);
	}

	@Test
	void shouldNotAllowStableLifecycleSummaryWhenAdmissionRejected() {
		AssessmentLifecyclePipelineResult result = pipeline.run(input(
				assessmentPipelineResult(completeCollectedResult(), RuntimeState.DEGRADED),
				null,
				knownRollback(),
				knownVerification(true),
				verifiedLifecycleAudit()
		));

		assertThat(result.actionAdmissionDecision().admitted()).isFalse();
		assertThat(result.lifecycleSummary().status()).isNotEqualTo(
				ReliabilityLifecycleSummaryStatus.STABLE
		);
	}

	@Test
	void shouldNotAllowStableLifecycleSummaryWhenReadinessRejected() {
		AssessmentLifecyclePipelineResult result = pipeline.run(input(
				assessmentPipelineResult(completeCollectedResult(), RuntimeState.DEGRADED),
				knownScenario(),
				knownRollback(),
				knownVerification(true),
				incompleteLifecycleAudit()
		));

		assertThat(result.executionReadinessDecision().ready()).isFalse();
		assertThat(result.lifecycleSummary().status()).isNotEqualTo(
				ReliabilityLifecycleSummaryStatus.STABLE
		);
	}

	@Test
	void shouldNotTrustLifecycleSummaryWhenAuditIntegrityIncomplete() {
		AssessmentLifecyclePipelineResult result = pipeline.run(input(
				assessmentPipelineResult(completeCollectedResult(), RuntimeState.DEGRADED),
				knownScenario(),
				knownRollback(),
				knownVerification(true),
				incompleteLifecycleAudit()
		));

		assertThat(result.lifecycleSummary().trusted()).isFalse();
	}

	@Test
	void shouldNotExposeRawPayloadOrVendorDetail() {
		AssessmentLifecyclePipelineResult result = pipeline.run(input(
				assessmentPipelineResult(completeCollectedResult(), RuntimeState.DEGRADED),
				knownScenario(),
				knownRollback(),
				knownVerification(true),
				verifiedLifecycleAudit()
		));

		assertThat(result.exposesRawPayload()).isFalse();
		assertThat(result.assessmentPipelineResult().collectionResult()
				.adapterResults()).isEmpty();
	}

	@Test
	void shouldNotMutatePortfolioKnowledgeSource() {
		AssessmentLifecyclePipelineInput input = input(
				assessmentPipelineResult(completeCollectedResult(), RuntimeState.DEGRADED),
				knownScenario(),
				knownRollback(),
				knownVerification(true),
				verifiedLifecycleAudit()
		);
		AssessmentLifecyclePipelineResult result = pipeline.run(input);

		assertThat(input.mutatesPortfolioKnowledgeSource()).isFalse();
		assertThat(result.mutatesPortfolioKnowledgeSource()).isFalse();
	}

	@Test
	void shouldRejectNullInput() {
		assertThatThrownBy(() -> pipeline.run(null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("input must not be null");
	}

	private AssessmentLifecyclePipelineInput input(
			EvidenceAssessmentPipelineResult assessmentPipelineResult,
			ScenarioReference scenarioReference,
			RollbackReference rollbackReference,
			VerificationReference verificationReference,
			LifecycleAuditDecision lifecycleAuditDecision
	) {
		return new AssessmentLifecyclePipelineInput(
				assessmentPipelineResult,
				scenarioReference,
				rollbackReference,
				verificationReference,
				true,
				true,
				verificationReference != null
						&& verificationReference.paymentConsistencyVerification(),
				false,
				true,
				true,
				true,
				true,
				lifecycleAuditDecision
		);
	}

	private EvidenceAssessmentPipelineResult assessmentPipelineResult(
			EvidenceCollectionResult collectionResult,
			RuntimeState runtimeState
	) {
		return evidenceAssessmentPipeline.run(new EvidenceAssessmentPipelineInput(
				collectionResult,
				runtimeState,
				PropagationSignal.CROSS_SERVICE,
				false,
				false,
				new ConvergenceWindow(
						Duration.ofMinutes(5),
						Duration.ofMinutes(5)
				),
				List.of()
		));
	}

	private EvidenceCollectionResult completeCollectedResult() {
		return new EvidenceCollectionResult(
				List.of(
						EvidenceCollectionStage.QUERY_DISPATCH,
						EvidenceCollectionStage.ADAPTER_COLLECTION,
						EvidenceCollectionStage.SIGNAL_NORMALIZATION,
						EvidenceCollectionStage.RESULT_AGGREGATION
				),
				List.of(),
				List.of(
						signal(EvidenceSignalType.METRIC, "metric-1", "metric"),
						signal(EvidenceSignalType.LOG, "log-1", "log"),
						signal(EvidenceSignalType.TRACE, "trace-1", "trace"),
						signal(EvidenceSignalType.TIMELINE, "timeline-1", "timeline"),
						signal(EvidenceSignalType.VERIFICATION, "verification-1", "verification"),
						signal(EvidenceSignalType.PAYMENT_SAFETY, "payment-1", "payment")
				),
				EvidenceCollectionStatus.COLLECTED,
				false,
				false,
				OperationalUncertainty.LOW,
				null
		);
	}

	private EvidenceCollectionResult collectedWithoutPaymentSafety() {
		return new EvidenceCollectionResult(
				List.of(
						EvidenceCollectionStage.QUERY_DISPATCH,
						EvidenceCollectionStage.ADAPTER_COLLECTION,
						EvidenceCollectionStage.SIGNAL_NORMALIZATION,
						EvidenceCollectionStage.RESULT_AGGREGATION
				),
				List.of(),
				List.of(
						signal(EvidenceSignalType.METRIC, "metric-1", "metric"),
						signal(EvidenceSignalType.LOG, "log-1", "log"),
						signal(EvidenceSignalType.TRACE, "trace-1", "trace"),
						signal(EvidenceSignalType.TIMELINE, "timeline-1", "timeline"),
						signal(EvidenceSignalType.VERIFICATION, "verification-1", "verification")
				),
				EvidenceCollectionStatus.COLLECTED,
				true,
				false,
				OperationalUncertainty.CRITICAL,
				null
		);
	}

	private EvidenceCollectionResult contradictoryCollectedResult() {
		return new EvidenceCollectionResult(
				List.of(
						EvidenceCollectionStage.QUERY_DISPATCH,
						EvidenceCollectionStage.ADAPTER_COLLECTION,
						EvidenceCollectionStage.SIGNAL_NORMALIZATION,
						EvidenceCollectionStage.RESULT_AGGREGATION
				),
				List.of(),
				List.of(
						signal(EvidenceSignalType.LOG, "shared-1", "healthy"),
						signal(EvidenceSignalType.LOG, "shared-1", "degraded"),
						signal(EvidenceSignalType.VERIFICATION, "verification-1", "verification"),
						signal(EvidenceSignalType.PAYMENT_SAFETY, "payment-1", "payment")
				),
				EvidenceCollectionStatus.PARTIAL,
				false,
				true,
				OperationalUncertainty.HIGH,
				null
		);
	}

	private ScenarioReference knownScenario() {
		return new ScenarioReference(
				"scenario-1",
				"portfolio-runtime",
				true,
				false
		);
	}

	private RollbackReference knownRollback() {
		return new RollbackReference(
				"rollback-1",
				"portfolio-runtime",
				true,
				false
		);
	}

	private VerificationReference knownVerification(
			boolean paymentConsistencyVerification
	) {
		return new VerificationReference(
				"verification-1",
				"portfolio-runtime",
				true,
				false,
				paymentConsistencyVerification
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

	private OperationalUncertainty map(ReliabilityRiskLevel level) {
		return switch (level) {
			case LOW -> OperationalUncertainty.LOW;
			case MEDIUM -> OperationalUncertainty.MODERATE;
			case HIGH -> OperationalUncertainty.HIGH;
			case CRITICAL -> OperationalUncertainty.CRITICAL;
		};
	}

	private EvidenceSignal signal(
			EvidenceSignalType type,
			String signalId,
			String summary
	) {
		return new EvidenceSignal(type, signalId, summary);
	}
}
