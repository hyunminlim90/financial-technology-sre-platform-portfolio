package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class OperationalReliabilityExecutionReadinessSkeletonTest {

	private final ExecutionReadinessGate gate = new ExecutionReadinessGate();
	private final ReliabilityAssessmentOrchestrator orchestrator =
			new ReliabilityAssessmentOrchestrator(new VerificationGate());
	private final ReliabilityRiskClassifier riskClassifier =
			new ReliabilityRiskClassifier();
	private final HumanApprovalPolicy humanApprovalPolicy =
			new HumanApprovalPolicy();
	private final ReliabilityRecommendationBoundary recommendationBoundary =
			new ReliabilityRecommendationBoundary();
	private final ActionCommandBoundary actionCommandBoundary =
			new ActionCommandBoundary();
	private final ScenarioBinding scenarioBinding = new ScenarioBinding();
	private final RollbackVerificationBinding rollbackVerificationBinding =
			new RollbackVerificationBinding();
	private final SafetyPolicyGate safetyPolicyGate = new SafetyPolicyGate();
	private final ActionAdmissionGate actionAdmissionGate = new ActionAdmissionGate();
	private final ExecutionBoundary executionBoundary = new ExecutionBoundary();
	private final ReliabilityExecutorContract executorContract =
			new ReliabilityExecutorContract();

	@Test
	void shouldRejectWhenExecutionBoundaryRejected() {
		ExecutionReadinessDecision decision = gate.evaluate(new ExecutionReadinessRequirement(
				rejectedBoundary(),
				validPlanFor(eligibleExecutionDecision()),
				verifiedAudit(false)
		));

		assertThat(decision.ready()).isFalse();
		assertThat(decision.rejectionReason()).isEqualTo(
				ExecutionReadinessRejectionReason.EXECUTION_BOUNDARY_REJECTED
		);
	}

	@Test
	void shouldRejectWhenExecutionPlanRejected() {
		ExecutionBoundaryDecision boundaryDecision = eligibleExecutionDecision();
		ExecutionPlan rejectedPlan = executorContract.plan(new ExecutionIntent(
				boundaryDecision,
				null,
				"verification-plan-1",
				false,
				false,
				false,
				true
		));

		ExecutionReadinessDecision decision = gate.evaluate(new ExecutionReadinessRequirement(
				boundaryDecision,
				rejectedPlan,
				verifiedAudit(false)
		));

		assertThat(decision.ready()).isFalse();
		assertThat(decision.rejectionReason())
				.isEqualTo(ExecutionReadinessRejectionReason.EXECUTION_PLAN_REJECTED);
	}

	@Test
	void shouldRejectWhenAuditIntegrityIsIncomplete() {
		ExecutionBoundaryDecision boundaryDecision = eligibleExecutionDecision();

		ExecutionReadinessDecision decision = gate.evaluate(new ExecutionReadinessRequirement(
				boundaryDecision,
				validPlanFor(boundaryDecision),
				new ExecutionAuditDecision(
						ExecutionAuditTrail.empty(),
						ExecutionAuditIntegrity.INCOMPLETE
				)
		));

		assertThat(decision.ready()).isFalse();
		assertThat(decision.rejectionReason()).isEqualTo(
				ExecutionReadinessRejectionReason.AUDIT_INTEGRITY_INCOMPLETE
		);
	}

	@Test
	void shouldRequireAuditAndPaymentConsistencyForPaymentImpactingPlan() {
		ExecutionBoundaryDecision boundaryDecision = eligibleExecutionDecision();
		ExecutionPlan plan = new ExecutionPlan(
				ExecutionPlanStatus.STRUCTURED,
				new ExecutionIntent(
						boundaryDecision,
						"rollback-plan-1",
						"verification-plan-1",
						true,
						false,
						false,
						true
				),
				"rollback-plan-1",
				"verification-plan-1",
				null
		);

		ExecutionReadinessDecision decision = gate.evaluate(new ExecutionReadinessRequirement(
				boundaryDecision,
				plan,
				verifiedAudit(true)
		));

		assertThat(decision.ready()).isFalse();
		assertThat(decision.rejectionReason()).isEqualTo(
				ExecutionReadinessRejectionReason
						.MISSING_PAYMENT_CONSISTENCY_VERIFICATION
		);
	}

	@Test
	void shouldRejectAiOnlyDecisionAsNotReady() {
		ExecutionBoundaryDecision boundaryDecision = eligibleExecutionDecision();
		ExecutionPlan plan = new ExecutionPlan(
				ExecutionPlanStatus.STRUCTURED,
				new ExecutionIntent(
						boundaryDecision,
						"rollback-plan-1",
						"verification-plan-1",
						false,
						false,
						true,
						true
				),
				"rollback-plan-1",
				"verification-plan-1",
				null
		);

		ExecutionReadinessDecision decision = gate.evaluate(new ExecutionReadinessRequirement(
				boundaryDecision,
				plan,
				verifiedAudit(false)
		));

		assertThat(decision.ready()).isFalse();
		assertThat(decision.rejectionReason()).isEqualTo(
				ExecutionReadinessRejectionReason.AI_ONLY_DECISION_NOT_ACCEPTABLE
		);
	}

	@Test
	void shouldRequireAllCriticalPrerequisitesForCriticalReadiness() {
		ExecutionBoundaryDecision criticalBoundary = criticalEligibleBoundary();
		ExecutionPlan criticalPlan = validPlanFor(criticalBoundary);

		ExecutionReadinessDecision decision = gate.evaluate(new ExecutionReadinessRequirement(
				criticalBoundary,
				criticalPlan,
				verifiedAudit(true)
		));

		assertThat(decision.ready()).isTrue();
		assertThat(decision.scope()).isEqualTo(ExecutionReadinessScope.READY);
	}

	@Test
	void shouldRemainSemanticGateOnlyEvenWhenReady() {
		ExecutionBoundaryDecision boundaryDecision = eligibleExecutionDecision();

		ExecutionReadinessDecision decision = gate.evaluate(new ExecutionReadinessRequirement(
				boundaryDecision,
				validPlanFor(boundaryDecision),
				verifiedAudit(false)
		));

		assertThat(decision.ready()).isTrue();
		assertThat(decision.semanticGateOnly()).isTrue();
		assertThat(decision.executes()).isFalse();
	}

	@Test
	void shouldRejectNullRequirement() {
		assertThatThrownBy(() -> gate.evaluate(null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("requirement must not be null");
	}

	private ExecutionBoundaryDecision rejectedBoundary() {
		return new ExecutionBoundaryDecision(
				false,
				ExecutionScope.NONE,
				new ExecutionRequirement(
						eligibleActionAdmission(),
						false,
						false,
						false,
						false
				),
				ExecutionBoundaryRejectionReason.ACTION_ADMISSION_NOT_ACCEPTED
		);
	}

	private ExecutionBoundaryDecision eligibleExecutionDecision() {
		return executionBoundary.evaluate(new ExecutionRequirement(
				eligibleActionAdmission(),
				true,
				true,
				true,
				true
		));
	}

	private ExecutionBoundaryDecision criticalEligibleBoundary() {
		return executionBoundary.evaluate(new ExecutionRequirement(
				criticalActionAdmission(),
				true,
				true,
				true,
				true
		));
	}

	private ExecutionPlan validPlanFor(ExecutionBoundaryDecision boundaryDecision) {
		return executorContract.plan(new ExecutionIntent(
				boundaryDecision,
				"rollback-plan-1",
				"verification-plan-1",
				boundaryDecision.requirement().critical(),
				boundaryDecision.requirement().critical(),
				false,
				true
		));
	}

	private ExecutionAuditDecision verifiedAudit(boolean paymentImpacting) {
		ExecutionAuditTrail trail = ExecutionAuditTrail.empty()
				.append(event(ExecutionAuditEventType.APPROVAL_RECORDED, "approval-1"))
				.append(event(ExecutionAuditEventType.ELIGIBILITY_RECORDED, "eligibility-1"))
				.append(event(ExecutionAuditEventType.PLAN_CREATED, "plan-1"));
		return trail.verify(paymentImpacting);
	}

	private ActionAdmissionDecision eligibleActionAdmission() {
		return actionAdmissionFor(
				assess(RuntimeState.CONVERGED, completeEvidence(), false, false),
				false
		);
	}

	private ActionAdmissionDecision criticalActionAdmission() {
		return actionAdmissionFor(
				assess(RuntimeState.FAILED, completeEvidence(), false, false),
				true
		);
	}

	private ActionAdmissionDecision actionAdmissionFor(
			ReliabilityAssessmentResult assessmentResult,
			boolean critical
		) {
		ReliabilityRiskClassification riskClassification =
				riskClassifier.classify(assessmentResult);
		HumanApprovalDecision humanApprovalDecision = critical
				? humanApprovalPolicy.evaluate(assessmentResult, riskClassification)
				: new HumanApprovalDecision(
						false,
						HumanApprovalScope.OPTIONAL,
						new HumanApprovalRequirement(false, false, false, false),
						List.of(HumanApprovalReason.AI_ONLY_APPROVAL_IS_NOT_ALLOWED)
				);
		RecommendationEligibility recommendationEligibility = critical
				? recommendationBoundary.evaluate(
						assessmentResult,
						riskClassification,
						humanApprovalDecision
				)
				: new RecommendationEligibility(
						true,
						RecommendationScope.ADVISORY_WITH_ROLLBACK_AND_VERIFICATION_REQUIREMENT,
						true,
						true,
						List.of(RecommendationRestriction.EXECUTION_AUTHORITY_PROHIBITED),
						List.of(
								RecommendationBoundaryReason
										.ASSESSMENT_RESULT_IS_NOT_RECOMMENDATION,
								RecommendationBoundaryReason
										.AI_RECOMMENDATION_IS_ADVISORY_ONLY
						)
				);
		ActionCommandEligibility actionCommandEligibility = new ActionCommandEligibility(
				true,
				new ActionCommandRequirement(true, true, true),
				List.of(
						ActionCommandRestriction.RECOMMENDATION_IS_NOT_ACTION_COMMAND,
						ActionCommandRestriction
								.RECOMMENDATION_ELIGIBILITY_IS_NOT_ACTION_ELIGIBILITY
				),
				List.of(
						ActionCommandBoundaryReason.RECOMMENDATION_IS_NOT_ACTION_COMMAND,
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
						critical
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
						critical
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
		return orchestrator.assess(new ReliabilityAssessmentInput(
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

	private ExecutionAuditEvent event(ExecutionAuditEventType type, String id) {
		return new ExecutionAuditEvent(
				type,
				id,
				"summary-" + id,
				Instant.parse("2026-05-28T00:00:00Z")
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

	private EvidenceSignal signal(EvidenceSignalType type, String signalId) {
		return new EvidenceSignal(type, signalId, "summary-" + signalId);
	}
}
