package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

class OperationalReliabilityExecutorContractSkeletonTest {

	private final ReliabilityExecutorContract contract =
			new ReliabilityExecutorContract();
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

	@Test
	void shouldNotBeActualExecutorImplementation() {
		ExecutionPlan plan = contract.plan(validIntent());

		assertThat(plan.status()).isEqualTo(ExecutionPlanStatus.STRUCTURED);
		assertThat(plan.planOnly()).isTrue();
		assertThat(plan.executes()).isFalse();
	}

	@Test
	void shouldRejectPlanCreationWithoutExecutionEligibility() {
		ExecutionPlan plan = contract.plan(new ExecutionIntent(
				nonEligibleExecutionDecision(),
				"rollback-plan-1",
				"verification-plan-1",
				false,
				false,
				false,
				true
		));

		assertThat(plan.status()).isEqualTo(ExecutionPlanStatus.REJECTED);
		assertThat(plan.rejectionReason())
				.isEqualTo(ExecutionPlanRejectionReason.EXECUTION_NOT_ELIGIBLE);
	}

	@Test
	void shouldRequireRollbackPlanReference() {
		ExecutionPlan plan = contract.plan(new ExecutionIntent(
				eligibleExecutionDecision(),
				null,
				"verification-plan-1",
				false,
				false,
				false,
				true
		));

		assertThat(plan.status()).isEqualTo(ExecutionPlanStatus.REJECTED);
		assertThat(plan.rejectionReason())
				.isEqualTo(
						ExecutionPlanRejectionReason.MISSING_ROLLBACK_PLAN_REFERENCE
				);
	}

	@Test
	void shouldRequireVerificationPlanReference() {
		ExecutionPlan plan = contract.plan(new ExecutionIntent(
				eligibleExecutionDecision(),
				"rollback-plan-1",
				null,
				false,
				false,
				false,
				true
		));

		assertThat(plan.status()).isEqualTo(ExecutionPlanStatus.REJECTED);
		assertThat(plan.rejectionReason())
				.isEqualTo(
						ExecutionPlanRejectionReason
								.MISSING_VERIFICATION_PLAN_REFERENCE
				);
	}

	@Test
	void shouldRequirePaymentConsistencyVerificationForPaymentImpactingPlan() {
		ExecutionPlan plan = contract.plan(new ExecutionIntent(
				eligibleExecutionDecision(),
				"rollback-plan-1",
				"verification-plan-1",
				true,
				false,
				false,
				true
		));

		assertThat(plan.status()).isEqualTo(ExecutionPlanStatus.REJECTED);
		assertThat(plan.rejectionReason()).isEqualTo(
				ExecutionPlanRejectionReason
						.MISSING_PAYMENT_CONSISTENCY_VERIFICATION
		);
	}

	@Test
	void shouldTreatPlanCreationAsDifferentFromExecution() {
		ExecutionPlan plan = contract.plan(validIntent());

		assertThat(plan.status()).isEqualTo(ExecutionPlanStatus.STRUCTURED);
		assertThat(plan.planOnly()).isTrue();
		assertThat(plan.executes()).isFalse();
	}

	@Test
	void shouldStructurePlanOnlyAfterFinalPreExecutionStep() {
		ExecutionPlan plan = contract.plan(validIntent());

		assertThat(plan.status()).isEqualTo(ExecutionPlanStatus.STRUCTURED);
		assertThat(plan.rollbackPlanReference()).isEqualTo("rollback-plan-1");
		assertThat(plan.verificationPlanReference()).isEqualTo("verification-plan-1");
	}

	@Test
	void shouldRejectAiOnlyExecutionPlanApproval() {
		ExecutionPlan plan = contract.plan(new ExecutionIntent(
				eligibleExecutionDecision(),
				"rollback-plan-1",
				"verification-plan-1",
				false,
				false,
				true,
				true
		));

		assertThat(plan.status()).isEqualTo(ExecutionPlanStatus.REJECTED);
		assertThat(plan.rejectionReason())
				.isEqualTo(
						ExecutionPlanRejectionReason.AI_ONLY_APPROVAL_PROHIBITED
				);
	}

	@Test
	void shouldRejectWithoutExplicitExecutionAuthorization() {
		ExecutionPlan plan = contract.plan(new ExecutionIntent(
				eligibleExecutionDecision(),
				"rollback-plan-1",
				"verification-plan-1",
				false,
				false,
				false,
				false
		));

		assertThat(plan.status()).isEqualTo(ExecutionPlanStatus.REJECTED);
		assertThat(plan.rejectionReason()).isEqualTo(
				ExecutionPlanRejectionReason
						.MISSING_EXPLICIT_EXECUTION_AUTHORIZATION
		);
	}

	@Test
	void shouldRejectNullIntent() {
		assertThatThrownBy(() -> contract.plan(null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("intent must not be null");
	}

	private ExecutionIntent validIntent() {
		return new ExecutionIntent(
				eligibleExecutionDecision(),
				"rollback-plan-1",
				"verification-plan-1",
				true,
				true,
				false,
				true
		);
	}

	private ExecutionBoundaryDecision nonEligibleExecutionDecision() {
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

	private ActionAdmissionDecision eligibleActionAdmission() {
		ReliabilityAssessmentResult assessmentResult = assess(
				RuntimeState.CONVERGED,
				completeEvidence(),
				false,
				false
		);
		ReliabilityRiskClassification riskClassification =
				riskClassifier.classify(assessmentResult);
		HumanApprovalDecision humanApprovalDecision = new HumanApprovalDecision(
				false,
				HumanApprovalScope.OPTIONAL,
				new HumanApprovalRequirement(false, false, false, false),
				List.of(HumanApprovalReason.AI_ONLY_APPROVAL_IS_NOT_ALLOWED)
		);
		RecommendationEligibility recommendationEligibility =
				new RecommendationEligibility(
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
