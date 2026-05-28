package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class OperationalReliabilityLifecycleSummarySkeletonTest {

	private final ReliabilityLifecycleSummaryBuilder summaryBuilder =
			new ReliabilityLifecycleSummaryBuilder();
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
	void shouldRemainOperatorFacingReadModelOnly() {
		ReliabilityLifecycleSummary summary = summaryBuilder.build(
				stableLifecycleResult(),
				verifiedAuditDecision()
		);

		assertThat(summary.operatorFacingReadModel()).isTrue();
		assertThat(summary.recommendation()).isFalse();
		assertThat(summary.executionPermission()).isFalse();
	}

	@Test
	void shouldNotTrustSummaryWhenAuditIntegrityIncomplete() {
		ReliabilityLifecycleSummary summary = summaryBuilder.build(
				stableLifecycleResult(),
				incompleteAuditDecision()
		);

		assertThat(summary.trusted()).isFalse();
		assertThat(summary.status()).isEqualTo(ReliabilityLifecycleSummaryStatus.RECOVERED);
		assertThat(summary.reason()).isEqualTo(
				ReliabilityLifecycleSummaryReason.AUDIT_INTEGRITY_INCOMPLETE
		);
	}

	@Test
	void shouldEscalatePaymentInconsistencyToCriticalRisk() {
		ReliabilityLifecycleSummary summary = summaryBuilder.build(
				regressionLifecycleResult(
						new RegressionSignal(
								RegressionSignalType.PAYMENT_INCONSISTENCY,
								"payment-inconsistency-1",
								"payment inconsistency"
						)
				),
				verifiedAuditDecision()
		);

		assertThat(summary.risk()).isEqualTo(OperationalUncertainty.CRITICAL);
		assertThat(summary.reason()).isEqualTo(
				ReliabilityLifecycleSummaryReason.PAYMENT_INCONSISTENCY_DETECTED
		);
	}

	@Test
	void shouldNotAllowStableSummaryWhenRegressionDetected() {
		ReliabilityLifecycleSummary summary = summaryBuilder.build(
				regressionLifecycleResult(
						new RegressionSignal(
								RegressionSignalType.SERVICE_DEGRADATION,
								"degradation-1",
								"service degradation"
						)
				),
				verifiedAuditDecision()
		);

		assertThat(summary.status()).isEqualTo(ReliabilityLifecycleSummaryStatus.UNCERTAIN);
		assertThat(summary.reason()).isEqualTo(
				ReliabilityLifecycleSummaryReason.REGRESSION_DETECTED
		);
	}

	@Test
	void shouldNotTreatExecutorSuccessAloneAsRecoveredSummary() {
		ReliabilityLifecycleSummary summary = summaryBuilder.build(
				successButVerificationIncompleteLifecycleResult(),
				verifiedAuditDecision()
		);

		assertThat(summary.status()).isEqualTo(ReliabilityLifecycleSummaryStatus.UNCERTAIN);
		assertThat(summary.reason()).isEqualTo(
				ReliabilityLifecycleSummaryReason.EXECUTION_ACKNOWLEDGEMENT_ONLY
		);
	}

	@Test
	void shouldAllowStableSummaryOnlyForConvergedNoRegressionAndCompleteAudit() {
		ReliabilityLifecycleSummary summary = summaryBuilder.build(
				stableLifecycleResult(),
				verifiedAuditDecision()
		);

		assertThat(summary.status()).isEqualTo(ReliabilityLifecycleSummaryStatus.STABLE);
		assertThat(summary.trusted()).isTrue();
		assertThat(summary.reason()).isEqualTo(
				ReliabilityLifecycleSummaryReason.STABLE_CONVERGENCE_CONFIRMED
		);
	}

	@Test
	void shouldKeepUncertaintySummaryForFailedExecutorResponse() {
		ReliabilityLifecycleSummary summary = summaryBuilder.build(
				failedLifecycleResult(),
				verifiedAuditDecision()
		);

		assertThat(summary.status()).isEqualTo(ReliabilityLifecycleSummaryStatus.FAILED);
		assertThat(summary.reason()).isEqualTo(
				ReliabilityLifecycleSummaryReason.EXECUTOR_RESPONSE_FAILED
		);
	}

	@Test
	void shouldKeepUncertaintySummaryForUnknownExecutorResponse() {
		ReliabilityLifecycleSummary summary = summaryBuilder.build(
				unknownLifecycleResult(),
				verifiedAuditDecision()
		);

		assertThat(summary.status()).isEqualTo(ReliabilityLifecycleSummaryStatus.UNCERTAIN);
		assertThat(summary.reason()).isEqualTo(
				ReliabilityLifecycleSummaryReason.EXECUTOR_RESPONSE_UNKNOWN
		);
	}

	@Test
	void shouldRejectNullInputs() {
		assertThatThrownBy(() -> summaryBuilder.build(null, verifiedAuditDecision()))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("lifecycleResult must not be null");
		assertThatThrownBy(() -> summaryBuilder.build(stableLifecycleResult(), null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("lifecycleAuditDecision must not be null");
	}

	private ReliabilityLifecycleResult stableLifecycleResult() {
		return lifecycleResult(
				new ExecutorResponse(ExecutorStatus.SUCCESS, "exec-1", "execution success"),
				completeEvidenceCorrelation(),
				true,
				false,
				false,
				List.of()
		);
	}

	private ReliabilityLifecycleResult successButVerificationIncompleteLifecycleResult() {
		return lifecycleResult(
				new ExecutorResponse(ExecutorStatus.SUCCESS, "exec-2", "execution success"),
				evidenceWithoutVerification(),
				true,
				false,
				false,
				List.of()
		);
	}

	private ReliabilityLifecycleResult regressionLifecycleResult(
			RegressionSignal signal
	) {
		return lifecycleResult(
				new ExecutorResponse(ExecutorStatus.SUCCESS, "exec-3", "execution success"),
				completeEvidenceCorrelation(),
				true,
				false,
				false,
				List.of(signal)
		);
	}

	private ReliabilityLifecycleResult failedLifecycleResult() {
		return lifecycleResult(
				new ExecutorResponse(ExecutorStatus.FAILURE, "exec-4", "execution failed"),
				completeEvidenceCorrelation(),
				true,
				false,
				false,
				List.of()
		);
	}

	private ReliabilityLifecycleResult unknownLifecycleResult() {
		return lifecycleResult(
				new ExecutorResponse(ExecutorStatus.UNKNOWN, "exec-5", "execution unknown"),
				completeEvidenceCorrelation(),
				true,
				false,
				false,
				List.of()
		);
	}

	private ReliabilityLifecycleResult lifecycleResult(
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
						verifiedExecutionAuditDecision(
								executionPlan.intent().paymentImpacting()
						)
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

		return lifecycleOrchestrator.orchestrate(new ReliabilityLifecycleInput(
				assessmentResult,
				actionAdmissionDecision,
				executionReadinessDecision,
				executorResponse,
				verificationDecision,
				convergenceDecision,
				regressionDecision
		));
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
								ActionCommandRestriction.RECOMMENDATION_IS_NOT_ACTION_COMMAND,
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

	private LifecycleAuditDecision verifiedAuditDecision() {
		return completeLifecycleTrail().verify(true);
	}

	private ExecutionAuditDecision verifiedExecutionAuditDecision(
			boolean paymentImpacting
	) {
		ExecutionAuditTrail trail = ExecutionAuditTrail.empty()
				.append(executionAuditEvent(
						ExecutionAuditEventType.APPROVAL_RECORDED,
						"approval-1"
				))
				.append(executionAuditEvent(
						ExecutionAuditEventType.ELIGIBILITY_RECORDED,
						"eligibility-1"
				))
				.append(executionAuditEvent(
						ExecutionAuditEventType.PLAN_CREATED,
						"plan-1"
				));
		return trail.verify(paymentImpacting);
	}

	private LifecycleAuditDecision incompleteAuditDecision() {
		return LifecycleAuditTrail.empty()
				.append(event(LifecycleAuditEventType.ASSESSMENT_RECORDED, "assessment-1"))
				.verify(false);
	}

	private LifecycleAuditTrail completeLifecycleTrail() {
		return LifecycleAuditTrail.empty()
				.append(event(LifecycleAuditEventType.ASSESSMENT_RECORDED, "assessment-1"))
				.append(event(LifecycleAuditEventType.ADMISSION_RECORDED, "admission-1"))
				.append(event(LifecycleAuditEventType.READINESS_RECORDED, "readiness-1"))
				.append(event(
						LifecycleAuditEventType.EXECUTOR_RESPONSE_RECORDED,
						"executor-1"
				))
				.append(event(
						LifecycleAuditEventType.POST_VERIFICATION_RECORDED,
						"verification-1"
				))
				.append(event(
						LifecycleAuditEventType.POST_CONVERGENCE_RECORDED,
						"convergence-1"
				))
				.append(event(
						LifecycleAuditEventType.POST_REGRESSION_RECORDED,
						"regression-1"
				));
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

	private LifecycleAuditEvent event(
			LifecycleAuditEventType type,
			String id
	) {
		return new LifecycleAuditEvent(
				type,
				id,
				"summary-" + id,
				Instant.parse("2026-05-28T00:00:00Z")
		);
	}

	private ExecutionAuditEvent executionAuditEvent(
			ExecutionAuditEventType type,
			String id
	) {
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
