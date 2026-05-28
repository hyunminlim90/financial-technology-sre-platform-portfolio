package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

class OperationalReliabilityActionAdmissionSkeletonTest {

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
	private final ActionAdmissionGate gate = new ActionAdmissionGate();

	@Test
	void shouldRejectWhenSafetyPolicyRejects() {
		ActionAdmissionDecision decision = gate.evaluate(requirement(
				assess(
						RuntimeState.VERIFIED,
						List.of(
								signal(EvidenceSignalType.METRIC, "metric-1"),
								signal(EvidenceSignalType.LOG, "log-1"),
								signal(EvidenceSignalType.VERIFICATION, "verification-1"),
								signal(EvidenceSignalType.PAYMENT_SAFETY, "payment-1")
						),
						false,
						true
				),
				scenarioBinding.bind(knownScenario()),
				rollbackVerificationBinding.bind(
						rollbackReference(true, false),
						verificationReference(true, false, true),
						false
				),
				false,
				false,
				false,
				false
		));

		assertThat(decision.admitted()).isFalse();
		assertThat(decision.rejectionReason())
				.isEqualTo(ActionAdmissionRejectionReason.SAFETY_POLICY_REJECTED);
	}

	@Test
	void shouldRejectWhenActionCommandBoundaryRejects() {
		ActionAdmissionDecision decision = gate.evaluate(requirementForcingSafetyPass(
				assess(RuntimeState.CONVERGED, completeEvidence(), false, false),
				rejectedActionEligibility(),
				eligibleRecommendation(),
				scenarioBinding.bind(knownScenario()),
				rollbackBound(false)
		));

		assertThat(decision.admitted()).isFalse();
		assertThat(decision.rejectionReason())
				.isEqualTo(
						ActionAdmissionRejectionReason.ACTION_COMMAND_BOUNDARY_REJECTED
				);
	}

	@Test
	void shouldRejectWhenRecommendationBoundaryRejects() {
		ActionAdmissionDecision decision = gate.evaluate(requirementForcingSafetyPass(
				assess(RuntimeState.CONVERGED, completeEvidence(), false, false),
				eligibleActionEligibility(),
				rejectedRecommendation(),
				scenarioBinding.bind(knownScenario()),
				rollbackBound(false)
		));

		assertThat(decision.admitted()).isFalse();
		assertThat(decision.rejectionReason())
				.isEqualTo(
						ActionAdmissionRejectionReason.RECOMMENDATION_BOUNDARY_REJECTED
				);
	}

	@Test
	void shouldRejectWhenScenarioBindingRejects() {
		ActionAdmissionDecision decision = gate.evaluate(requirementForcingSafetyPass(
				assess(RuntimeState.CONVERGED, completeEvidence(), false, false),
				eligibleActionEligibility(),
				eligibleRecommendation(),
				scenarioBinding.bind(null),
				rollbackBound(false)
		));

		assertThat(decision.admitted()).isFalse();
		assertThat(decision.rejectionReason())
				.isEqualTo(ActionAdmissionRejectionReason.SCENARIO_BINDING_REJECTED);
	}

	@Test
	void shouldRejectWhenRollbackVerificationBindingRejects() {
		ActionAdmissionDecision decision = gate.evaluate(requirementForcingSafetyPass(
				assess(RuntimeState.CONVERGED, completeEvidence(), false, false),
				eligibleActionEligibility(),
				eligibleRecommendation(),
				scenarioBinding.bind(knownScenario()),
				rollbackVerificationBinding.bind(
						null,
						verificationReference(true, false, true),
						false
				)
		));

		assertThat(decision.admitted()).isFalse();
		assertThat(decision.rejectionReason()).isEqualTo(
				ActionAdmissionRejectionReason
						.ROLLBACK_VERIFICATION_BINDING_REJECTED
		);
	}

	@Test
	void shouldRejectUnrestrictedAdmissionWhenStateIsRestricted() {
		ActionAdmissionDecision decision = gate.evaluate(requirementForcingSafetyPass(
				assess(RuntimeState.CONVERGED, completeEvidence(), false, false),
				eligibleActionEligibility(),
				eligibleRecommendation(),
				scenarioBinding.bind(deprecatedScenario()),
				rollbackBound(false),
				true
		));

		assertThat(decision.admitted()).isFalse();
		assertThat(decision.rejectionReason()).isEqualTo(
				ActionAdmissionRejectionReason
						.RESTRICTED_STATE_DISALLOWS_UNRESTRICTED_ADMISSION
		);
	}

	@Test
	void shouldIncludeApprovalRequirementForHighRisk() {
		ActionAdmissionRequirement requirement = requirement(
				assess(
						RuntimeState.VERIFIED,
						List.of(
								signal(EvidenceSignalType.METRIC, "metric-1"),
								signal(EvidenceSignalType.LOG, "log-1"),
								signal(EvidenceSignalType.VERIFICATION, "verification-1"),
								signal(EvidenceSignalType.PAYMENT_SAFETY, "payment-1")
						),
						false,
						true
				),
				scenarioBinding.bind(knownScenario()),
				rollbackBound(false),
				true,
				false,
				false,
				false
		);

		assertThat(requirement.approvalRequired()).isTrue();
	}

	@Test
	void shouldIncludeExplicitApprovalAndRollbackVerificationReviewForCritical() {
		ActionAdmissionRequirement requirement = requirement(
				assess(RuntimeState.FAILED, completeEvidence(), false, false),
				scenarioBinding.bind(knownScenario()),
				rollbackBound(false),
				true,
				true,
				false,
				false
		);

		assertThat(requirement.explicitApprovalRequired()).isTrue();
		assertThat(requirement.rollbackReviewRequired()).isTrue();
		assertThat(requirement.verificationReviewRequired()).isTrue();
	}

	@Test
	void shouldRemainCandidateAdmissionOnlyEvenWhenAdmitted() {
		ActionAdmissionDecision decision = gate.evaluate(requirementForcingSafetyPass(
				assess(RuntimeState.CONVERGED, completeEvidence(), false, false),
				eligibleActionEligibility(),
				eligibleRecommendation(),
				scenarioBinding.bind(knownScenario()),
				rollbackBound(false)
		));

		assertThat(decision.admitted()).isTrue();
		assertThat(decision.candidateAdmissionOnly()).isTrue();
		assertThat(decision.executionPermission()).isFalse();
		assertThat(decision.scope()).isEqualTo(ActionAdmissionScope.CANDIDATE);
	}

	@Test
	void shouldRejectNullRequirement() {
		assertThatThrownBy(() -> gate.evaluate(null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("requirement must not be null");
	}

	private ActionAdmissionRequirement requirement(
			ReliabilityAssessmentResult assessmentResult,
			ScenarioBindingDecision scenarioBindingDecision,
			RollbackVerificationBindingDecision rollbackVerificationBindingDecision,
			boolean approvalProvided,
			boolean explicitApprovalProvided,
			boolean paymentSafetyAction,
			boolean unrestrictedRequested
		) {
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
		ActionCommandEligibility actionCommandEligibility =
				actionCommandBoundary.evaluate(
						assessmentResult,
						riskClassification,
						humanApprovalDecision,
						recommendationEligibility
				);
		SafetyPolicyDecision safetyPolicyDecision = safetyPolicyGate.evaluate(
				new SafetyPolicyRequirement(
						assessmentResult,
						riskClassification,
						humanApprovalDecision,
						scenarioBindingDecision,
						rollbackVerificationBindingDecision,
						approvalProvided,
						explicitApprovalProvided,
						paymentSafetyAction
				)
		);

		return new ActionAdmissionRequirement(
				riskClassification,
				humanApprovalDecision,
				recommendationEligibility,
				actionCommandEligibility,
				scenarioBindingDecision,
				rollbackVerificationBindingDecision,
				safetyPolicyDecision,
				unrestrictedRequested
		);
	}

	private ActionAdmissionRequirement requirementForcingSafetyPass(
			ReliabilityAssessmentResult assessmentResult,
			ActionCommandEligibility actionCommandEligibility,
			RecommendationEligibility recommendationEligibility,
			ScenarioBindingDecision scenarioBindingDecision,
			RollbackVerificationBindingDecision rollbackVerificationBindingDecision
		) {
		return requirementForcingSafetyPass(
				assessmentResult,
				actionCommandEligibility,
				recommendationEligibility,
				scenarioBindingDecision,
				rollbackVerificationBindingDecision,
				false
		);
	}

	private ActionAdmissionRequirement requirementForcingSafetyPass(
			ReliabilityAssessmentResult assessmentResult,
			ActionCommandEligibility actionCommandEligibility,
			RecommendationEligibility recommendationEligibility,
			ScenarioBindingDecision scenarioBindingDecision,
			RollbackVerificationBindingDecision rollbackVerificationBindingDecision,
			boolean unrestrictedRequested
		) {
		ReliabilityRiskClassification riskClassification =
				riskClassifier.classify(assessmentResult);
		HumanApprovalDecision humanApprovalDecision = new HumanApprovalDecision(
				false,
				HumanApprovalScope.OPTIONAL,
				new HumanApprovalRequirement(false, false, false, false),
				List.of(HumanApprovalReason.AI_ONLY_APPROVAL_IS_NOT_ALLOWED)
		);
		SafetyPolicyDecision safetyPolicyDecision = new SafetyPolicyDecision(
				true,
				(scenarioBindingDecision.highRiskRestricted()
						|| rollbackVerificationBindingDecision.highRiskRestricted())
								? SafetyPolicyScope.RESTRICTED
								: SafetyPolicyScope.STANDARD,
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

		return new ActionAdmissionRequirement(
				riskClassification,
				humanApprovalDecision,
				recommendationEligibility,
				actionCommandEligibility,
				scenarioBindingDecision,
				rollbackVerificationBindingDecision,
				safetyPolicyDecision,
				unrestrictedRequested
		);
	}

	private RecommendationEligibility eligibleRecommendation() {
		return new RecommendationEligibility(
				true,
				RecommendationScope.ADVISORY_WITH_ROLLBACK_AND_VERIFICATION_REQUIREMENT,
				true,
				true,
				List.of(RecommendationRestriction.EXECUTION_AUTHORITY_PROHIBITED),
				List.of(
						RecommendationBoundaryReason.ASSESSMENT_RESULT_IS_NOT_RECOMMENDATION,
						RecommendationBoundaryReason.AI_RECOMMENDATION_IS_ADVISORY_ONLY
				)
		);
	}

	private RecommendationEligibility rejectedRecommendation() {
		return new RecommendationEligibility(
				false,
				RecommendationScope.NONE,
				false,
				false,
				List.of(RecommendationRestriction.NO_RECOMMENDATION_AVAILABLE),
				List.of(RecommendationBoundaryReason.NO_RECOMMENDATION_SCENARIO)
		);
	}

	private ActionCommandEligibility eligibleActionEligibility() {
		return new ActionCommandEligibility(
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
	}

	private ActionCommandEligibility rejectedActionEligibility() {
		return new ActionCommandEligibility(
				false,
				new ActionCommandRequirement(true, true, true),
				List.of(ActionCommandRestriction.NO_ACTION_COMMAND_AVAILABLE),
				List.of(ActionCommandBoundaryReason.NO_ACTION_COMMAND_SCENARIO)
		);
	}

	private RollbackVerificationBindingDecision rollbackBound(
			boolean restricted
	) {
		return new RollbackVerificationBindingDecision(
				restricted
						? RollbackVerificationBindingStatus.RESTRICTED
						: RollbackVerificationBindingStatus.BOUND,
				rollbackReference(true, restricted),
				verificationReference(true, false, true),
				restricted
						? RollbackVerificationBindingRejectionReason
								.DEPRECATED_ROLLBACK_HIGH_RISK_RESTRICTION
						: null
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
