package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

class OperationalReliabilityActionCommandBoundarySkeletonTest {

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

	@Test
	void shouldTreatRecommendationAsDifferentFromActionCommand() {
		ActionCommandEligibility eligibility = evaluate(
				assess(
						RuntimeState.CONVERGED,
						completeEvidence(),
						false,
						false,
						List.of()
				)
		);

		assertThat(eligibility.reasons())
				.contains(ActionCommandBoundaryReason.RECOMMENDATION_IS_NOT_ACTION_COMMAND);
	}

	@Test
	void shouldRejectAdvisoryRecommendationAsExecutableActionCommand() {
		ActionCommandEligibility eligibility = evaluate(
				assess(
						RuntimeState.CONVERGED,
						completeEvidence(),
						false,
						false,
						List.of()
				)
		);

		assertThat(eligibility.eligible()).isFalse();
		assertThat(eligibility.restrictions())
				.contains(
						ActionCommandRestriction.ADVISORY_ONLY_RECOMMENDATION_NOT_EXECUTABLE
				);
	}

	@Test
	void shouldTreatRecommendationEligibilityAsDifferentFromActionEligibility() {
		ActionCommandEligibility eligibility = evaluate(
				assess(
						RuntimeState.CONVERGED,
						completeEvidence(),
						false,
						false,
						List.of()
				)
		);

		assertThat(eligibility.restrictions())
				.contains(
						ActionCommandRestriction
								.RECOMMENDATION_ELIGIBILITY_IS_NOT_ACTION_ELIGIBILITY
				);
	}

	@Test
	void shouldBlockAutomaticActionCommandWhenHumanApprovalIsRequired() {
		ActionCommandEligibility eligibility = evaluate(
				assess(
						RuntimeState.VERIFIED,
						List.of(
								signal(EvidenceSignalType.METRIC, "metric-1"),
								signal(EvidenceSignalType.LOG, "log-1"),
								signal(EvidenceSignalType.VERIFICATION, "verification-1"),
								signal(EvidenceSignalType.PAYMENT_SAFETY, "payment-1")
						),
						false,
						true,
						List.of()
				)
		);

		assertThat(eligibility.restrictions())
				.contains(
						ActionCommandRestriction
								.HUMAN_APPROVAL_REQUIRED_BLOCKS_AUTOMATIC_ACTION_COMMAND
				);
		assertThat(eligibility.requirement().humanApprovalSatisfied()).isFalse();
	}

	@Test
	void shouldBlockActionCommandWithoutRollbackRequirement() {
		ActionCommandEligibility eligibility = evaluate(
				assess(
						RuntimeState.CONVERGED,
						completeEvidence(),
						false,
						false,
						List.of()
				)
		);

		assertThat(eligibility.restrictions())
				.contains(ActionCommandRestriction.MISSING_ROLLBACK_REQUIREMENT);
	}

	@Test
	void shouldBlockActionCommandWithoutVerificationRequirement() {
		ActionCommandEligibility eligibility = evaluate(
				assess(
						RuntimeState.CONVERGED,
						completeEvidence(),
						false,
						false,
						List.of()
				)
		);

		assertThat(eligibility.restrictions())
				.contains(ActionCommandRestriction.MISSING_VERIFICATION_REQUIREMENT);
	}

	@Test
	void shouldBlockActionCommandUnderPaymentSafetyUncertainty() {
		ActionCommandEligibility eligibility = evaluate(
				assess(
						RuntimeState.VERIFIED,
						List.of(
								signal(EvidenceSignalType.METRIC, "metric-1"),
								signal(EvidenceSignalType.LOG, "log-1"),
								signal(EvidenceSignalType.TRACE, "trace-1"),
								signal(EvidenceSignalType.TIMELINE, "timeline-1"),
								signal(EvidenceSignalType.VERIFICATION, "verification-1")
						),
						false,
						false,
						List.of()
				)
		);

		assertThat(eligibility.restrictions())
				.contains(
						ActionCommandRestriction
								.PAYMENT_SAFETY_UNCERTAINTY_BLOCKS_ACTION_COMMAND
				);
	}

	@Test
	void shouldBlockActionCommandWhenEvidenceIsContradictory() {
		ActionCommandEligibility eligibility = evaluate(
				assess(
						RuntimeState.CONVERGED,
						completeEvidence(),
						true,
						false,
						List.of()
				)
		);

		assertThat(eligibility.restrictions())
				.contains(
						ActionCommandRestriction.CONTRADICTORY_EVIDENCE_BLOCKS_ACTION_COMMAND
				);
	}

	@Test
	void shouldBlockUnrestrictedActionCommandForFailedOrCriticalStates() {
		ActionCommandEligibility eligibility = evaluate(
				assess(
						RuntimeState.FAILED,
						completeEvidence(),
						false,
						false,
						List.of()
				)
		);

		assertThat(eligibility.restrictions())
				.contains(
						ActionCommandRestriction
								.FAILED_OR_CRITICAL_UNRESTRICTED_ACTION_COMMAND_PROHIBITED
				);
	}

	@Test
	void shouldReturnNoActionCommandWhenNoScenarioExists() {
		ActionCommandEligibility eligibility = evaluate(
				assess(
						RuntimeState.UNKNOWN,
						List.of(),
						false,
						false,
						List.of()
				)
		);

		assertThat(eligibility.eligible()).isFalse();
		assertThat(eligibility.restrictions())
				.contains(ActionCommandRestriction.NO_ACTION_COMMAND_AVAILABLE);
	}

	@Test
	void shouldRemainSemanticAdmissionControlOnly() {
		ActionCommandEligibility eligibility = evaluate(
				assess(
						RuntimeState.CONVERGED,
						completeEvidence(),
						false,
						false,
						List.of()
				)
		);

		assertThat(eligibility.semanticAdmissionOnly()).isTrue();
		assertThat(eligibility.executable()).isFalse();
	}

	@Test
	void shouldRejectNullAssessmentResult() {
		ReliabilityAssessmentResult assessmentResult = assess(
				RuntimeState.CONVERGED,
				completeEvidence(),
				false,
				false,
				List.of()
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

		assertThatThrownBy(() -> actionCommandBoundary.evaluate(
				null,
				riskClassification,
				humanApprovalDecision,
				recommendationEligibility
		))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("assessmentResult must not be null");
	}

	@Test
	void shouldRejectNullRiskClassification() {
		ReliabilityAssessmentResult assessmentResult = assess(
				RuntimeState.CONVERGED,
				completeEvidence(),
				false,
				false,
				List.of()
		);
		HumanApprovalDecision humanApprovalDecision = humanApprovalPolicy.evaluate(
				assessmentResult,
				riskClassifier.classify(assessmentResult)
		);
		RecommendationEligibility recommendationEligibility =
				recommendationBoundary.evaluate(
						assessmentResult,
						riskClassifier.classify(assessmentResult),
						humanApprovalDecision
				);

		assertThatThrownBy(() -> actionCommandBoundary.evaluate(
				assessmentResult,
				null,
				humanApprovalDecision,
				recommendationEligibility
		))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("riskClassification must not be null");
	}

	@Test
	void shouldRejectNullHumanApprovalDecision() {
		ReliabilityAssessmentResult assessmentResult = assess(
				RuntimeState.CONVERGED,
				completeEvidence(),
				false,
				false,
				List.of()
		);
		ReliabilityRiskClassification riskClassification =
				riskClassifier.classify(assessmentResult);
		RecommendationEligibility recommendationEligibility =
				recommendationBoundary.evaluate(
						assessmentResult,
						riskClassification,
						humanApprovalPolicy.evaluate(assessmentResult, riskClassification)
				);

		assertThatThrownBy(() -> actionCommandBoundary.evaluate(
				assessmentResult,
				riskClassification,
				null,
				recommendationEligibility
		))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("humanApprovalDecision must not be null");
	}

	@Test
	void shouldRejectNullRecommendationEligibility() {
		ReliabilityAssessmentResult assessmentResult = assess(
				RuntimeState.CONVERGED,
				completeEvidence(),
				false,
				false,
				List.of()
		);
		ReliabilityRiskClassification riskClassification =
				riskClassifier.classify(assessmentResult);
		HumanApprovalDecision humanApprovalDecision = humanApprovalPolicy.evaluate(
				assessmentResult,
				riskClassification
		);

		assertThatThrownBy(() -> actionCommandBoundary.evaluate(
				assessmentResult,
				riskClassification,
				humanApprovalDecision,
				null
		))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("recommendationEligibility must not be null");
	}

	private ActionCommandEligibility evaluate(
			ReliabilityAssessmentResult assessmentResult
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
		return actionCommandBoundary.evaluate(
				assessmentResult,
				riskClassification,
				humanApprovalDecision,
				recommendationEligibility
		);
	}

	private ReliabilityAssessmentResult assess(
			RuntimeState runtimeState,
			List<EvidenceSignal> evidenceSignals,
			boolean contradictoryEvidence,
			boolean propagationActive,
			List<RegressionSignal> regressionSignals
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
				regressionSignals
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
