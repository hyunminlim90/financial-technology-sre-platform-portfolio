package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class OperationalReliabilityLifecycleOrchestratorSkeletonTest {

	private final ReliabilityLifecycleOrchestrator lifecycleOrchestrator =
			new ReliabilityLifecycleOrchestrator();
	private final ReliabilityAssessmentOrchestrator assessmentOrchestrator =
			new ReliabilityAssessmentOrchestrator(new VerificationGate());
	private final ReliabilityRiskClassifier riskClassifier =
			new ReliabilityRiskClassifier();
	private final HumanApprovalPolicy humanApprovalPolicy =
			new HumanApprovalPolicy();
	private final ReliabilityRecommendationBoundary recommendationBoundary =
			new ReliabilityRecommendationBoundary();
	private final ScenarioBinding scenarioBinding = new ScenarioBinding();
	private final RollbackVerificationBinding rollbackVerificationBinding =
			new RollbackVerificationBinding();
	private final SafetyPolicyGate safetyPolicyGate = new SafetyPolicyGate();
	private final ActionAdmissionGate actionAdmissionGate = new ActionAdmissionGate();
	private final ExecutionBoundary executionBoundary = new ExecutionBoundary();
	private final ReliabilityExecutorContract executorContract =
			new ReliabilityExecutorContract();
	private final ExecutionReadinessGate executionReadinessGate =
			new ExecutionReadinessGate();
	private final PostExecutionVerification postExecutionVerification =
			new PostExecutionVerification();
	private final PostExecutionConvergence postExecutionConvergence =
			new PostExecutionConvergence();
	private final PostExecutionRegression postExecutionRegression =
			new PostExecutionRegression();

	@Test
	void shouldPreserveFixedLifecycleStageOrder() {
		ReliabilityLifecycleResult result = lifecycleOrchestrator.orchestrate(
				stableInput()
		);

		assertThat(result.stages()).containsExactly(
				ReliabilityLifecycleStage.PRE_EXECUTION_ASSESSMENT,
				ReliabilityLifecycleStage.ACTION_ADMISSION,
				ReliabilityLifecycleStage.EXECUTION_READINESS,
				ReliabilityLifecycleStage.EXECUTOR_RESPONSE,
				ReliabilityLifecycleStage.POST_EXECUTION_VERIFICATION,
				ReliabilityLifecycleStage.POST_EXECUTION_CONVERGENCE,
				ReliabilityLifecycleStage.POST_EXECUTION_REGRESSION
		);
		assertThat(result.stable()).isTrue();
	}

	@Test
	void shouldNotTreatExecutorSuccessAsVerifiedOrConverged() {
		ReliabilityLifecycleResult result = lifecycleOrchestrator.orchestrate(
				successButVerificationIncompleteInput()
		);

		assertThat(result.executorResponse().status()).isEqualTo(ExecutorStatus.SUCCESS);
		assertThat(result.lifecycleState()).isEqualTo(RuntimeState.VERIFYING);
		assertThat(result.postExecutionVerificationDecision().verified()).isFalse();
		assertThat(result.postExecutionConvergenceDecision().converged()).isFalse();
		assertThat(result.stable()).isFalse();
		assertThat(result.rejectionReason()).isEqualTo(
				ReliabilityLifecycleRejectionReason.POST_EXECUTION_VERIFICATION_REQUIRED
		);
	}

	@Test
	void shouldRequirePostExecutionVerificationBeforeConvergence() {
		ReliabilityLifecycleInput input = successButVerificationIncompleteInput();
		PostExecutionConvergenceDecision forcedConvergedDecision =
				new PostExecutionConvergenceDecision(
						PostExecutionConvergenceStatus.CONVERGED,
						input.postExecutionConvergenceDecision().requirement(),
						null
				);

		ReliabilityLifecycleResult result = lifecycleOrchestrator.orchestrate(
				new ReliabilityLifecycleInput(
						input.assessmentResult(),
						input.actionAdmissionDecision(),
						input.executionReadinessDecision(),
						input.executorResponse(),
						input.postExecutionVerificationDecision(),
						forcedConvergedDecision,
						input.postExecutionRegressionDecision()
				)
		);

		assertThat(result.stable()).isFalse();
		assertThat(result.rejectionReason()).isEqualTo(
				ReliabilityLifecycleRejectionReason
						.CONVERGENCE_REQUIRES_POST_EXECUTION_VERIFICATION
		);
	}

	@Test
	void shouldMarkLifecycleAsNotStableWhenPostExecutionRegressionDetected() {
		ReliabilityLifecycleResult result = lifecycleOrchestrator.orchestrate(
				regressionDetectedInput(
						new RegressionSignal(
								RegressionSignalType.SERVICE_DEGRADATION,
								"degradation-1",
								"service degradation"
						)
				)
		);

		assertThat(result.postExecutionConvergenceDecision().converged()).isTrue();
		assertThat(result.postExecutionRegressionDecision().regressionDetected()).isTrue();
		assertThat(result.lifecycleState()).isEqualTo(RuntimeState.DEGRADED);
		assertThat(result.stable()).isFalse();
		assertThat(result.rejectionReason()).isEqualTo(
				ReliabilityLifecycleRejectionReason.POST_EXECUTION_REGRESSION_DETECTED
		);
	}

	@Test
	void shouldEscalatePaymentInconsistencyToCriticalLifecycleRisk() {
		ReliabilityLifecycleResult result = lifecycleOrchestrator.orchestrate(
				regressionDetectedInput(
						new RegressionSignal(
								RegressionSignalType.PAYMENT_INCONSISTENCY,
								"payment-inconsistency-1",
								"payment inconsistency"
						)
				)
		);

		assertThat(result.postExecutionRegressionDecision().regressionDetected()).isTrue();
		assertThat(result.overallRisk()).isEqualTo(OperationalUncertainty.CRITICAL);
		assertThat(result.stable()).isFalse();
	}

	@Test
	void shouldKeepFailureAndUncertaintyForFailedExecutorResponse() {
		ReliabilityLifecycleResult result = lifecycleOrchestrator.orchestrate(
				executorFailureInput()
		);

		assertThat(result.executorResponse().status()).isEqualTo(ExecutorStatus.FAILURE);
		assertThat(result.lifecycleState()).isEqualTo(RuntimeState.FAILED);
		assertThat(result.overallRisk().ordinal())
				.isGreaterThanOrEqualTo(OperationalUncertainty.HIGH.ordinal());
		assertThat(result.rejectionReason()).isEqualTo(
				ReliabilityLifecycleRejectionReason.EXECUTOR_RESPONSE_FAILED
		);
	}

	@Test
	void shouldKeepUncertaintyForUnknownExecutorResponse() {
		ReliabilityLifecycleResult result = lifecycleOrchestrator.orchestrate(
				executorUnknownInput()
		);

		assertThat(result.executorResponse().status()).isEqualTo(ExecutorStatus.UNKNOWN);
		assertThat(result.lifecycleState()).isEqualTo(RuntimeState.UNSTABLE);
		assertThat(result.overallRisk().ordinal())
				.isGreaterThanOrEqualTo(OperationalUncertainty.HIGH.ordinal());
		assertThat(result.rejectionReason()).isEqualTo(
				ReliabilityLifecycleRejectionReason.EXECUTOR_RESPONSE_UNKNOWN
		);
	}

	@Test
	void shouldRemainNonRecommendationAndNonExecutionPermission() {
		ReliabilityLifecycleResult result = lifecycleOrchestrator.orchestrate(
				stableInput()
		);

		assertThat(result.recommendation()).isFalse();
		assertThat(result.executionPermission()).isFalse();
	}

	@Test
	void shouldRejectNullInput() {
		assertThatThrownBy(() -> lifecycleOrchestrator.orchestrate(null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("input must not be null");
	}

	private ReliabilityLifecycleInput stableInput() {
		return lifecycleInput(
				new ExecutorResponse(ExecutorStatus.SUCCESS, "exec-1", "execution success"),
				completeEvidenceCorrelation(),
				true,
				false,
				false,
				List.of()
		);
	}

	private ReliabilityLifecycleInput successButVerificationIncompleteInput() {
		return lifecycleInput(
				new ExecutorResponse(ExecutorStatus.SUCCESS, "exec-2", "execution success"),
				evidenceWithoutVerification(),
				true,
				false,
				false,
				List.of()
		);
	}

	private ReliabilityLifecycleInput regressionDetectedInput(
			RegressionSignal signal
	) {
		return lifecycleInput(
				new ExecutorResponse(ExecutorStatus.SUCCESS, "exec-3", "execution success"),
				completeEvidenceCorrelation(),
				true,
				false,
				false,
				List.of(signal)
		);
	}

	private ReliabilityLifecycleInput executorFailureInput() {
		return lifecycleInput(
				new ExecutorResponse(ExecutorStatus.FAILURE, "exec-4", "execution failed"),
				completeEvidenceCorrelation(),
				true,
				false,
				false,
				List.of()
		);
	}

	private ReliabilityLifecycleInput executorUnknownInput() {
		return lifecycleInput(
				new ExecutorResponse(ExecutorStatus.UNKNOWN, "exec-5", "execution unknown"),
				completeEvidenceCorrelation(),
				true,
				false,
				false,
				List.of()
		);
	}

	private ReliabilityLifecycleInput lifecycleInput(
			ExecutorResponse executorResponse,
			EvidenceCorrelation evidenceCorrelation,
			boolean paymentConsistencyVerified,
			boolean rollbackTriggeredExecution,
			boolean rollbackVerified,
			List<RegressionSignal> regressionSignals
	) {
		ReliabilityAssessmentResult assessmentResult = assess(
				RuntimeState.CONVERGED,
				evidenceCorrelation.signals(),
				evidenceCorrelation.contradictoryEvidence(),
				false
		);
		ActionAdmissionDecision actionAdmissionDecision = eligibleActionAdmission(
				assessmentResult
		);
		ExecutionBoundaryDecision executionBoundaryDecision =
				executionBoundary.evaluate(
						new ExecutionRequirement(
								actionAdmissionDecision,
								true,
								true,
								true,
								true
						)
				);
		ExecutionPlan executionPlan = executorContract.plan(new ExecutionIntent(
				executionBoundaryDecision,
				"rollback-plan-1",
				"verification-plan-1",
				executionBoundaryDecision.requirement().critical(),
				executionBoundaryDecision.requirement().critical(),
				false,
				true
		));
		ExecutionReadinessDecision executionReadinessDecision =
				executionReadinessGate.evaluate(new ExecutionReadinessRequirement(
						executionBoundaryDecision,
						executionPlan,
						verifiedAudit(executionPlan.intent().paymentImpacting())
				));
		PostExecutionVerificationDecision verificationDecision =
				postExecutionVerification.verify(
						new PostExecutionVerificationRequirement(
								executorResponse,
								executionBoundaryDecision.requirement(),
								evidenceCorrelation,
								paymentConsistencyVerified,
								rollbackTriggeredExecution,
								rollbackVerified
						)
				);
		PostExecutionConvergenceDecision convergenceDecision =
				postExecutionConvergence.assess(new PostExecutionConvergenceRequirement(
						verificationDecision,
						new ConvergenceWindow(
								Duration.ofMinutes(5),
								Duration.ofMinutes(5)
						),
						PropagationSignal.CROSS_SERVICE,
						false
				));
		PostExecutionRegressionDecision regressionDecision =
				postExecutionRegression.detect(
						new PostExecutionRegressionRequirement(
								convergenceDecision,
								regressionSignals
						)
				);

		return new ReliabilityLifecycleInput(
				assessmentResult,
				actionAdmissionDecision,
				executionReadinessDecision,
				executorResponse,
				verificationDecision,
				convergenceDecision,
				regressionDecision
		);
	}

	private ActionAdmissionDecision eligibleActionAdmission(
			ReliabilityAssessmentResult assessmentResult
	) {
		ReliabilityRiskClassification riskClassification =
				riskClassifier.classify(assessmentResult);
		HumanApprovalDecision humanApprovalDecision = new HumanApprovalDecision(
				false,
				HumanApprovalScope.OPTIONAL,
				new HumanApprovalRequirement(false, false, false, false),
				List.of(HumanApprovalReason.AI_ONLY_APPROVAL_IS_NOT_ALLOWED)
		);
		RecommendationEligibility recommendationEligibility =
				recommendationBoundary.evaluate(
						assessmentResult,
						riskClassification,
						humanApprovalDecision
				);
		ActionCommandEligibility actionCommandEligibility =
				new ActionCommandEligibility(
						true,
						new ActionCommandRequirement(true, true, true),
						List.of(
								ActionCommandRestriction
										.RECOMMENDATION_IS_NOT_ACTION_COMMAND,
								ActionCommandRestriction
										.RECOMMENDATION_ELIGIBILITY_IS_NOT_ACTION_ELIGIBILITY
						),
						List.of(
								ActionCommandBoundaryReason
										.RECOMMENDATION_IS_NOT_ACTION_COMMAND,
								ActionCommandBoundaryReason
										.RECOMMENDATION_ELIGIBILITY_DOES_NOT_GRANT_ACTION_ELIGIBILITY
						)
				);
		ScenarioBindingDecision scenarioBindingDecision = scenarioBinding.bind(
				new ScenarioReference("scenario-known", "portfolio-runtime", true, false)
		);
		RollbackVerificationBindingDecision rollbackVerificationBindingDecision =
				rollbackVerificationBinding.bind(
						new RollbackReference(
								"rollback-1",
								"portfolio-runtime",
								true,
								false
						),
						new VerificationReference(
								"verification-1",
								"portfolio-runtime",
								true,
								false,
								true
						),
						false
				);
		SafetyPolicyDecision safetyPolicyDecision = safetyPolicyGate.evaluate(
				new SafetyPolicyRequirement(
						assessmentResult,
						riskClassification,
						humanApprovalDecision,
						scenarioBindingDecision,
						rollbackVerificationBindingDecision,
						true,
						true,
						false
				)
		);
		return actionAdmissionGate.evaluate(new ActionAdmissionRequirement(
				riskClassification,
				humanApprovalDecision,
				recommendationEligibility,
				actionCommandEligibility,
				scenarioBindingDecision,
				rollbackVerificationBindingDecision,
				safetyPolicyDecision,
				false
		));
	}

	private ReliabilityAssessmentResult assess(
			RuntimeState runtimeState,
			List<EvidenceSignal> evidenceSignals,
			boolean contradictoryEvidence,
			boolean propagationActive
	) {
		return assessmentOrchestrator.assess(new ReliabilityAssessmentInput(
				runtimeState,
				evidenceSignals,
				contradictoryEvidence,
				PropagationSignal.CROSS_SERVICE,
				propagationActive,
				false,
				new ConvergenceWindow(
						Duration.ofMinutes(5),
						Duration.ofMinutes(5)
				),
				List.of()
		));
	}

	private ExecutionAuditDecision verifiedAudit(boolean paymentImpacting) {
		ExecutionAuditTrail trail = ExecutionAuditTrail.empty()
				.append(event(ExecutionAuditEventType.APPROVAL_RECORDED, "approval-1"))
				.append(event(ExecutionAuditEventType.ELIGIBILITY_RECORDED, "eligibility-1"))
				.append(event(ExecutionAuditEventType.PLAN_CREATED, "plan-1"));
		return trail.verify(paymentImpacting);
	}

	private EvidenceCorrelation completeEvidenceCorrelation() {
		return EvidenceCorrelation.correlate(completeEvidence(), false);
	}

	private EvidenceCorrelation evidenceWithoutVerification() {
		return EvidenceCorrelation.correlate(
				List.of(
						signal(EvidenceSignalType.METRIC, "metric-1"),
						signal(EvidenceSignalType.LOG, "log-1"),
						signal(EvidenceSignalType.TRACE, "trace-1"),
						signal(EvidenceSignalType.TIMELINE, "timeline-1"),
						signal(EvidenceSignalType.PAYMENT_SAFETY, "payment-1")
				),
				false
		);
	}

	private List<EvidenceSignal> completeEvidence() {
		return List.of(
				signal(EvidenceSignalType.METRIC, "metric-1"),
				signal(EvidenceSignalType.LOG, "log-1"),
				signal(EvidenceSignalType.TRACE, "trace-1"),
				signal(EvidenceSignalType.TIMELINE, "timeline-1"),
				signal(EvidenceSignalType.VERIFICATION, "verification-1"),
				signal(EvidenceSignalType.PAYMENT_SAFETY, "payment-1")
		);
	}

	private ExecutionAuditEvent event(ExecutionAuditEventType type, String id) {
		return new ExecutionAuditEvent(
				type,
				id,
				"summary-" + id,
				Instant.parse("2026-05-28T00:00:00Z")
		);
	}

	private EvidenceSignal signal(EvidenceSignalType type, String signalId) {
		return new EvidenceSignal(type, signalId, "summary-" + signalId);
	}
}
