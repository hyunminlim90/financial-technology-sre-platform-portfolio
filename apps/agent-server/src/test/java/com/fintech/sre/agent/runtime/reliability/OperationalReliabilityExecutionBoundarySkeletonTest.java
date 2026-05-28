package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

class OperationalReliabilityExecutionBoundarySkeletonTest {

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
	private final ExecutionBoundary boundary = new ExecutionBoundary();

	@Test
	void shouldRejectEvenWhenActionAdmissionIsAcceptedWithoutExecutionAuthorization() {
		ExecutionBoundaryDecision decision = boundary.evaluate(requirement(
				admittedRestrictedOrStandardAction(false),
				false,
				false,
				true,
				true
		));

		assertThat(decision.executionEligible()).isFalse();
		assertThat(decision.rejectionReason()).isEqualTo(
				ExecutionBoundaryRejectionReason
						.EXPLICIT_EXECUTION_AUTHORIZATION_MISSING
		);
	}

	@Test
	void shouldRejectWhenApprovalIsRequiredButIncomplete() {
		ExecutionBoundaryDecision decision = boundary.evaluate(requirement(
				admittedCriticalAction(),
				true,
				false,
				true,
				true
		));

		assertThat(decision.executionEligible()).isFalse();
		assertThat(decision.rejectionReason())
				.isEqualTo(ExecutionBoundaryRejectionReason.APPROVAL_NOT_COMPLETED);
	}

	@Test
	void shouldRejectWhenRollbackReviewIsIncomplete() {
		ExecutionBoundaryDecision decision = boundary.evaluate(requirement(
				admittedCriticalAction(),
				true,
				true,
				false,
				true
		));

		assertThat(decision.executionEligible()).isFalse();
		assertThat(decision.rejectionReason()).isEqualTo(
				ExecutionBoundaryRejectionReason.ROLLBACK_REVIEW_NOT_COMPLETED
		);
	}

	@Test
	void shouldRejectWhenVerificationReviewIsIncomplete() {
		ExecutionBoundaryDecision decision = boundary.evaluate(requirement(
				admittedCriticalAction(),
				true,
				true,
				true,
				false
		));

		assertThat(decision.executionEligible()).isFalse();
		assertThat(decision.rejectionReason()).isEqualTo(
				ExecutionBoundaryRejectionReason
						.VERIFICATION_REVIEW_NOT_COMPLETED
		);
	}

	@Test
	void shouldRejectWhenPaymentSafetyIsUncertain() {
		ExecutionBoundaryDecision decision = boundary.evaluate(requirement(
				admittedActionWithPaymentUncertainty(),
				true,
				true,
				true,
				true
		));

		assertThat(decision.executionEligible()).isFalse();
		assertThat(decision.rejectionReason())
				.isEqualTo(ExecutionBoundaryRejectionReason.PAYMENT_SAFETY_UNCERTAINTY);
	}

	@Test
	void shouldRejectWhenEvidenceIsContradictory() {
		ExecutionBoundaryDecision decision = boundary.evaluate(requirement(
				admittedActionWithContradictoryEvidence(),
				true,
				true,
				true,
				true
		));

		assertThat(decision.executionEligible()).isFalse();
		assertThat(decision.rejectionReason())
				.isEqualTo(ExecutionBoundaryRejectionReason.CONTRADICTORY_EVIDENCE);
	}

	@Test
	void shouldRequireAllCriticalExecutionPrerequisites() {
		ExecutionBoundaryDecision decision = boundary.evaluate(requirement(
				admittedCriticalAction(),
				true,
				true,
				true,
				true
		));

		assertThat(decision.executionEligible()).isTrue();
		assertThat(decision.scope()).isEqualTo(ExecutionScope.ELIGIBLE);
		assertThat(decision.requirement().actionAdmissionDecision().requirement()
				.explicitApprovalRequired()).isTrue();
		assertThat(decision.requirement().actionAdmissionDecision().requirement()
				.rollbackReviewRequired()).isTrue();
		assertThat(decision.requirement().actionAdmissionDecision().requirement()
				.verificationReviewRequired()).isTrue();
	}

	@Test
	void shouldRemainBoundaryOnlyAndNotExecutor() {
		ExecutionBoundaryDecision decision = boundary.evaluate(requirement(
				admittedCriticalAction(),
				true,
				true,
				true,
				true
		));

		assertThat(decision.executionEligible()).isTrue();
		assertThat(decision.boundaryOnly()).isTrue();
		assertThat(decision.executes()).isFalse();
	}

	@Test
	void shouldRejectNullRequirement() {
		assertThatThrownBy(() -> boundary.evaluate(null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("requirement must not be null");
	}

	private ExecutionRequirement requirement(
			ActionAdmissionDecision actionAdmissionDecision,
			boolean explicitExecutionAuthorized,
			boolean approvalCompleted,
			boolean rollbackReviewCompleted,
			boolean verificationReviewCompleted
		) {
		return new ExecutionRequirement(
				actionAdmissionDecision,
				explicitExecutionAuthorized,
				approvalCompleted,
				rollbackReviewCompleted,
				verificationReviewCompleted
		);
	}

	private ActionAdmissionDecision admittedRestrictedOrStandardAction(
			boolean restricted
	) {
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
		ScenarioBindingDecision scenarioBindingDecision = restricted
				? scenarioBinding.bind(deprecatedScenario())
				: scenarioBinding.bind(knownScenario());
		RollbackVerificationBindingDecision rollbackVerificationBindingDecision =
				rollbackVerificationBinding.bind(
						rollbackReference(true, restricted),
						verificationReference(true, false, true),
						false
				);
		SafetyPolicyDecision safetyPolicyDecision = new SafetyPolicyDecision(
				true,
				restricted ? SafetyPolicyScope.RESTRICTED : SafetyPolicyScope.STANDARD,
				new SafetyPolicyRequirement(
						assessmentResult,
						riskClassification,
						humanApprovalDecision,
						scenarioBindingDecision,
						rollbackVerificationBindingDecision,
						true,
						true,
						false
				),
				null
		);
		return new ActionAdmissionDecision(
				true,
				restricted
						? ActionAdmissionScope.RESTRICTED_CANDIDATE
						: ActionAdmissionScope.CANDIDATE,
				new ActionAdmissionRequirement(
						riskClassification,
						humanApprovalDecision,
						recommendationEligibility,
						actionCommandEligibility,
						scenarioBindingDecision,
						rollbackVerificationBindingDecision,
						safetyPolicyDecision,
						false
				),
				null
		);
	}

	private ActionAdmissionDecision admittedCriticalAction() {
		ReliabilityAssessmentResult assessmentResult = assess(
				RuntimeState.FAILED,
				completeEvidence(),
				false,
				false
		);
		ReliabilityRiskClassification riskClassification =
				riskClassifier.classify(assessmentResult);
		HumanApprovalDecision humanApprovalDecision = humanApprovalPolicy.evaluate(
				assessmentResult,
				riskClassification
		);
		RecommendationEligibility recommendationEligibility =
				recommendationBoundary.evaluate(
						assessmentResult,
						riskClassification,
						humanApprovalDecision
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
				knownScenario()
		);
		RollbackVerificationBindingDecision rollbackVerificationBindingDecision =
				rollbackVerificationBinding.bind(
						rollbackReference(true, false),
						verificationReference(true, false, true),
						false
				);
		SafetyPolicyDecision safetyPolicyDecision = new SafetyPolicyDecision(
				true,
				SafetyPolicyScope.STANDARD,
				new SafetyPolicyRequirement(
						assessmentResult,
						riskClassification,
						humanApprovalDecision,
						scenarioBindingDecision,
						rollbackVerificationBindingDecision,
						true,
						true,
						false
				),
				null
		);
		return new ActionAdmissionDecision(
				true,
				ActionAdmissionScope.CANDIDATE,
				new ActionAdmissionRequirement(
						riskClassification,
						humanApprovalDecision,
						recommendationEligibility,
						actionCommandEligibility,
						scenarioBindingDecision,
						rollbackVerificationBindingDecision,
						safetyPolicyDecision,
						false
				),
				null
		);
	}

	private ActionAdmissionDecision admittedActionWithPaymentUncertainty() {
		ReliabilityAssessmentResult assessmentResult = assess(
				RuntimeState.VERIFIED,
				List.of(
						signal(EvidenceSignalType.METRIC, "metric-1"),
						signal(EvidenceSignalType.LOG, "log-1"),
						signal(EvidenceSignalType.TRACE, "trace-1"),
						signal(EvidenceSignalType.TIMELINE, "timeline-1"),
						signal(EvidenceSignalType.VERIFICATION, "verification-1")
				),
				false,
				false
		);
		return admittedActionFromAssessment(assessmentResult);
	}

	private ActionAdmissionDecision admittedActionWithContradictoryEvidence() {
		ReliabilityAssessmentResult assessmentResult = assess(
				RuntimeState.CONVERGED,
				completeEvidence(),
				true,
				false
		);
		return admittedActionFromAssessment(assessmentResult);
	}

	private ActionAdmissionDecision admittedActionFromAssessment(
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
				knownScenario()
		);
		RollbackVerificationBindingDecision rollbackVerificationBindingDecision =
				rollbackVerificationBinding.bind(
						rollbackReference(true, false),
						verificationReference(true, false, true),
						false
				);
		SafetyPolicyDecision safetyPolicyDecision = new SafetyPolicyDecision(
				true,
				SafetyPolicyScope.STANDARD,
				new SafetyPolicyRequirement(
						assessmentResult,
						riskClassification,
						humanApprovalDecision,
						scenarioBindingDecision,
						rollbackVerificationBindingDecision,
						true,
						true,
						false
				),
				null
		);
		return new ActionAdmissionDecision(
				true,
				ActionAdmissionScope.CANDIDATE,
				new ActionAdmissionRequirement(
						riskClassification,
						humanApprovalDecision,
						recommendationEligibility,
						actionCommandEligibility,
						scenarioBindingDecision,
						rollbackVerificationBindingDecision,
						safetyPolicyDecision,
						false
				),
				null
		);
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

	private ScenarioReference knownScenario() {
		return new ScenarioReference(
				"scenario-known",
				"portfolio-runtime",
				true,
				false
		);
	}

	private ScenarioReference deprecatedScenario() {
		return new ScenarioReference(
				"scenario-deprecated",
				"portfolio-runtime",
				true,
				true
		);
	}

	private RollbackReference rollbackReference(boolean known, boolean deprecated) {
		return new RollbackReference(
				"rollback-1",
				"portfolio-runtime",
				known,
				deprecated
		);
	}

	private VerificationReference verificationReference(
			boolean known,
			boolean deprecated,
			boolean paymentConsistencyVerification
		) {
		return new VerificationReference(
				"verification-1",
				"portfolio-runtime",
				known,
				deprecated,
				paymentConsistencyVerification
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
