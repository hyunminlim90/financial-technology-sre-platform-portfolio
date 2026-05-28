package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

class OperationalReliabilityRecommendationBoundarySkeletonTest {

	private final ReliabilityAssessmentOrchestrator orchestrator =
			new ReliabilityAssessmentOrchestrator(new VerificationGate());
	private final ReliabilityRiskClassifier riskClassifier =
			new ReliabilityRiskClassifier();
	private final HumanApprovalPolicy humanApprovalPolicy =
			new HumanApprovalPolicy();
	private final ReliabilityRecommendationBoundary boundary =
			new ReliabilityRecommendationBoundary();

	@Test
	void shouldTreatAssessmentResultAsNotBeingRecommendation() {
		RecommendationEligibility eligibility = evaluate(
				assess(
						RuntimeState.CONVERGED,
						completeEvidence(),
						false,
						false,
						List.of()
				)
		);

		assertThat(eligibility.reasons())
				.contains(
						RecommendationBoundaryReason
								.ASSESSMENT_RESULT_IS_NOT_RECOMMENDATION
				);
	}

	@Test
	void shouldKeepRecommendationEligibilitySeparateFromExecutionPermission() {
		RecommendationEligibility eligibility = evaluate(
				assess(
						RuntimeState.CONVERGED,
						completeEvidence(),
						false,
						false,
						List.of()
				)
		);

		assertThat(eligibility.eligible()).isTrue();
		assertThat(eligibility.executionPermission()).isFalse();
	}

	@Test
	void shouldRemainAdvisoryOnlyForAiRecommendations() {
		RecommendationEligibility eligibility = evaluate(
				assess(
						RuntimeState.CONVERGED,
						completeEvidence(),
						false,
						false,
						List.of()
				)
		);

		assertThat(eligibility.advisoryOnly()).isTrue();
		assertThat(eligibility.reasons())
				.contains(
						RecommendationBoundaryReason
								.AI_RECOMMENDATION_IS_ADVISORY_ONLY
				);
	}

	@Test
	void shouldProduceNoRecommendationWhenNoScenarioExists() {
		RecommendationEligibility eligibility = evaluate(
				assess(
						RuntimeState.UNKNOWN,
						List.of(),
						false,
						false,
						List.of()
				)
		);

		assertThat(eligibility.eligible()).isFalse();
		assertThat(eligibility.scope()).isEqualTo(RecommendationScope.NONE);
		assertThat(eligibility.restrictions())
				.contains(RecommendationRestriction.NO_RECOMMENDATION_AVAILABLE);
	}

	@Test
	void shouldRestrictUnsafeRecommendationUnderPaymentSafetyUncertainty() {
		RecommendationEligibility eligibility = evaluate(
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

		assertThat(eligibility.eligible()).isTrue();
		assertThat(eligibility.restrictions())
				.contains(RecommendationRestriction.UNSAFE_RECOMMENDATION_PROHIBITED);
		assertThat(eligibility.scope())
				.isEqualTo(RecommendationScope.HUMAN_APPROVAL_GATED_ADVISORY);
	}

	@Test
	void shouldRestrictAutomatedRecommendationWhenEvidenceIsContradictory() {
		RecommendationEligibility eligibility = evaluate(
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
						RecommendationRestriction.AUTOMATED_RECOMMENDATION_RESTRICTED
				);
	}

	@Test
	void shouldRestrictFailedAndCriticalStates() {
		RecommendationEligibility eligibility = evaluate(
				assess(
						RuntimeState.FAILED,
						completeEvidence(),
						false,
						false,
						List.of()
				)
		);

		assertThat(eligibility.eligible()).isTrue();
		assertThat(eligibility.restrictions())
				.contains(
						RecommendationRestriction.FAILED_STATE_RESTRICTED,
						RecommendationRestriction.CRITICAL_RISK_RESTRICTED
				);
	}

	@Test
	void shouldAllowRecommendationToCarryRollbackAndVerificationRequirements() {
		RecommendationEligibility eligibility = evaluate(
				assess(
						RuntimeState.FAILED,
						completeEvidence(),
						false,
						false,
						List.of()
				)
		);

		assertThat(eligibility.rollbackRequirement()).isTrue();
		assertThat(eligibility.verificationRequirement()).isTrue();
		assertThat(eligibility.scope()).isEqualTo(
				RecommendationScope.ADVISORY_WITH_ROLLBACK_AND_VERIFICATION_REQUIREMENT
		);
	}

	@Test
	void shouldPreventRecommendationFromBecomingExecutionAuthorityWhenHumanApprovalIsRequired() {
		RecommendationEligibility eligibility = evaluate(
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

		assertThat(eligibility.scope())
				.isEqualTo(RecommendationScope.HUMAN_APPROVAL_GATED_ADVISORY);
		assertThat(eligibility.executionPermission()).isFalse();
		assertThat(eligibility.restrictions())
				.contains(RecommendationRestriction.HUMAN_APPROVAL_REQUIRED);
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

		assertThatThrownBy(() -> boundary.evaluate(
				null,
				riskClassification,
				humanApprovalDecision
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

		assertThatThrownBy(() -> boundary.evaluate(
				assessmentResult,
				null,
				humanApprovalDecision
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

		assertThatThrownBy(() -> boundary.evaluate(
				assessmentResult,
				riskClassification,
				null
		))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("humanApprovalDecision must not be null");
	}

	private RecommendationEligibility evaluate(
			ReliabilityAssessmentResult assessmentResult
	) {
		ReliabilityRiskClassification riskClassification =
				riskClassifier.classify(assessmentResult);
		HumanApprovalDecision humanApprovalDecision = humanApprovalPolicy.evaluate(
				assessmentResult,
				riskClassification
		);
		return boundary.evaluate(
				assessmentResult,
				riskClassification,
				humanApprovalDecision
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
