package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

class OperationalReliabilityHumanApprovalPolicySkeletonTest {

	private final ReliabilityAssessmentOrchestrator orchestrator =
			new ReliabilityAssessmentOrchestrator(new VerificationGate());
	private final ReliabilityRiskClassifier riskClassifier =
			new ReliabilityRiskClassifier();
	private final HumanApprovalPolicy policy = new HumanApprovalPolicy();

	@Test
	void shouldRequireHumanApprovalForHighRiskOrAbove() {
		HumanApprovalDecision decision = decide(
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

		assertThat(decision.approvalRequired()).isTrue();
		assertThat(decision.scope()).isEqualTo(HumanApprovalScope.REQUIRED);
	}

	@Test
	void shouldRequireExplicitApprovalAndRollbackVerificationForCriticalRisk() {
		HumanApprovalDecision decision = decide(
				assess(
						RuntimeState.FAILED,
						completeEvidence(),
						false,
						false,
						List.of()
				)
		);

		assertThat(decision.approvalRequired()).isTrue();
		assertThat(decision.scope()).isEqualTo(HumanApprovalScope.CRITICAL_EXPLICIT);
		assertThat(decision.requirement().explicitHumanApprovalRequired()).isTrue();
		assertThat(decision.requirement().verificationReviewRequired()).isTrue();
		assertThat(decision.requirement().rollbackReviewRequired()).isTrue();
	}

	@Test
	void shouldRequireApprovalWhenPaymentSafetyUncertaintyExists() {
		HumanApprovalDecision decision = decide(
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

		assertThat(decision.approvalRequired()).isTrue();
		assertThat(decision.reasons())
				.contains(
						HumanApprovalReason
								.PAYMENT_SAFETY_UNCERTAINTY_REQUIRES_APPROVAL
				);
	}

	@Test
	void shouldRequireApprovalWhenEvidenceIsContradictory() {
		HumanApprovalDecision decision = decide(
				assess(
						RuntimeState.CONVERGED,
						completeEvidence(),
						true,
						false,
						List.of()
				)
		);

		assertThat(decision.approvalRequired()).isTrue();
		assertThat(decision.reasons())
				.contains(HumanApprovalReason.CONTRADICTORY_EVIDENCE_REQUIRES_APPROVAL);
	}

	@Test
	void shouldTreatLowRiskAsApprovalOptional() {
		HumanApprovalDecision decision = decide(
				assess(
						RuntimeState.CONVERGED,
						completeEvidence(),
						false,
						false,
						List.of()
				)
		);

		assertThat(decision.approvalRequired()).isFalse();
		assertThat(decision.scope()).isEqualTo(HumanApprovalScope.OPTIONAL);
		assertThat(decision.reasons())
				.contains(HumanApprovalReason.LOW_RISK_APPROVAL_IS_OPTIONAL);
	}

	@Test
	void shouldTreatMediumRiskAsContextDependentReviewCandidate() {
		HumanApprovalDecision decision = decide(
				assess(
						RuntimeState.UNKNOWN,
						completeEvidence(),
						false,
						false,
						List.of()
				)
		);

		assertThat(decision.approvalRequired()).isFalse();
		assertThat(decision.scope())
				.isEqualTo(HumanApprovalScope.CONTEXT_DEPENDENT_REVIEW);
		assertThat(decision.requirement().verificationReviewRequired()).isTrue();
	}

	@Test
	void shouldRemainSemanticGovernanceDecisionOnly() {
		HumanApprovalDecision decision = decide(
				assess(
						RuntimeState.CONVERGED,
						completeEvidence(),
						false,
						false,
						List.of()
				)
		);

		assertThat(decision.semanticGovernanceOnly()).isTrue();
		assertThat(decision.executionAuthorityGranted()).isFalse();
	}

	@Test
	void shouldDisallowAiOnlyApproval() {
		HumanApprovalDecision decision = decide(
				assess(
						RuntimeState.CONVERGED,
						completeEvidence(),
						false,
						false,
						List.of()
				)
		);

		assertThat(decision.requirement().aiOnlyApprovalAllowed()).isFalse();
		assertThat(decision.reasons())
				.contains(HumanApprovalReason.AI_ONLY_APPROVAL_IS_NOT_ALLOWED);
	}

	@Test
	void shouldRejectNullAssessmentResult() {
		ReliabilityRiskClassification lowRisk = riskClassifier.classify(
				assess(
						RuntimeState.CONVERGED,
						completeEvidence(),
						false,
						false,
						List.of()
				)
		);

		assertThatThrownBy(() -> policy.evaluate(null, lowRisk))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("assessmentResult must not be null");
	}

	@Test
	void shouldRejectNullRiskClassification() {
		ReliabilityAssessmentResult result = assess(
				RuntimeState.CONVERGED,
				completeEvidence(),
				false,
				false,
				List.of()
		);

		assertThatThrownBy(() -> policy.evaluate(result, null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("riskClassification must not be null");
	}

	private HumanApprovalDecision decide(
			ReliabilityAssessmentResult assessmentResult
	) {
		return policy.evaluate(
				assessmentResult,
				riskClassifier.classify(assessmentResult)
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
