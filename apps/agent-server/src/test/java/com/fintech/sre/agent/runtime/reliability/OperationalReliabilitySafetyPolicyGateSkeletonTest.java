package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

class OperationalReliabilitySafetyPolicyGateSkeletonTest {

	private final ReliabilityAssessmentOrchestrator orchestrator =
			new ReliabilityAssessmentOrchestrator(new VerificationGate());
	private final ReliabilityRiskClassifier riskClassifier =
			new ReliabilityRiskClassifier();
	private final HumanApprovalPolicy humanApprovalPolicy =
			new HumanApprovalPolicy();
	private final ScenarioBinding scenarioBinding = new ScenarioBinding();
	private final RollbackVerificationBinding rollbackVerificationBinding =
			new RollbackVerificationBinding();
	private final SafetyPolicyGate gate = new SafetyPolicyGate();

	@Test
	void shouldRejectWhenNoScenarioExists() {
		SafetyPolicyDecision decision = gate.evaluate(requirement(
				assess(RuntimeState.CONVERGED, completeEvidence(), false, false),
				scenarioBinding.bind(null),
				rollbackVerificationBinding.bind(
						rollbackReference(true, false),
						verificationReference(true, false, true),
						false
				),
				false,
				false,
				false
		));

		assertThat(decision.admitted()).isFalse();
		assertThat(decision.rejectionReason())
				.isEqualTo(SafetyPolicyRejectionReason.NO_SCENARIO);
	}

	@Test
	void shouldRejectWhenRollbackBindingIsMissing() {
		SafetyPolicyDecision decision = gate.evaluate(requirement(
				assess(RuntimeState.CONVERGED, completeEvidence(), false, false),
				scenarioBinding.bind(knownScenario()),
				rollbackVerificationBinding.bind(
						null,
						verificationReference(true, false, true),
						false
				),
				false,
				false,
				false
		));

		assertThat(decision.admitted()).isFalse();
		assertThat(decision.rejectionReason())
				.isEqualTo(SafetyPolicyRejectionReason.MISSING_ROLLBACK_BINDING);
	}

	@Test
	void shouldRejectWhenVerificationBindingIsMissing() {
		SafetyPolicyDecision decision = gate.evaluate(requirement(
				assess(RuntimeState.CONVERGED, completeEvidence(), false, false),
				scenarioBinding.bind(knownScenario()),
				rollbackVerificationBinding.bind(
						rollbackReference(true, false),
						null,
						false
				),
				false,
				false,
				false
		));

		assertThat(decision.admitted()).isFalse();
		assertThat(decision.rejectionReason())
				.isEqualTo(SafetyPolicyRejectionReason.MISSING_VERIFICATION_BINDING);
	}

	@Test
	void shouldRejectWhenPaymentSafetyIsUncertain() {
		SafetyPolicyDecision decision = gate.evaluate(requirement(
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
						false
				),
				scenarioBinding.bind(knownScenario()),
				rollbackVerificationBinding.bind(
						rollbackReference(true, false),
						verificationReference(true, false, true),
						false
				),
				false,
				false,
				false
		));

		assertThat(decision.admitted()).isFalse();
		assertThat(decision.rejectionReason())
				.isEqualTo(
						SafetyPolicyRejectionReason.PAYMENT_SAFETY_UNCERTAINTY
				);
	}

	@Test
	void shouldRejectWhenEvidenceIsContradictory() {
		SafetyPolicyDecision decision = gate.evaluate(requirement(
				assess(RuntimeState.CONVERGED, completeEvidence(), true, false),
				scenarioBinding.bind(knownScenario()),
				rollbackVerificationBinding.bind(
						rollbackReference(true, false),
						verificationReference(true, false, true),
						false
				),
				false,
				false,
				false
		));

		assertThat(decision.admitted()).isFalse();
		assertThat(decision.rejectionReason())
				.isEqualTo(SafetyPolicyRejectionReason.CONTRADICTORY_EVIDENCE);
	}

	@Test
	void shouldRejectWhenHighRiskApprovalIsMissing() {
		SafetyPolicyDecision decision = gate.evaluate(requirement(
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
				false
		));

		assertThat(decision.admitted()).isFalse();
		assertThat(decision.rejectionReason())
				.isEqualTo(SafetyPolicyRejectionReason.HIGH_RISK_APPROVAL_MISSING);
	}

	@Test
	void shouldRejectWhenCriticalExplicitApprovalIsMissing() {
		SafetyPolicyDecision decision = gate.evaluate(requirement(
				assess(RuntimeState.FAILED, completeEvidence(), false, false),
				scenarioBinding.bind(knownScenario()),
				rollbackVerificationBinding.bind(
						rollbackReference(true, false),
						verificationReference(true, false, true),
						false
				),
				true,
				false,
				false
		));

		assertThat(decision.admitted()).isFalse();
		assertThat(decision.rejectionReason()).isEqualTo(
				SafetyPolicyRejectionReason.CRITICAL_EXPLICIT_APPROVAL_MISSING
		);
	}

	@Test
	void shouldRestrictDeprecatedScenario() {
		SafetyPolicyDecision decision = gate.evaluate(requirement(
				assess(RuntimeState.CONVERGED, completeEvidence(), false, false),
				scenarioBinding.bind(deprecatedScenario()),
				rollbackVerificationBinding.bind(
						rollbackReference(true, false),
						verificationReference(true, false, true),
						false
				),
				true,
				true,
				false
		));

		assertThat(decision.admitted()).isTrue();
		assertThat(decision.scope()).isEqualTo(SafetyPolicyScope.RESTRICTED);
	}

	@Test
	void shouldRestrictDeprecatedRollbackOrVerificationBinding() {
		SafetyPolicyDecision rollbackRestrictedDecision = gate.evaluate(requirement(
				assess(RuntimeState.CONVERGED, completeEvidence(), false, false),
				scenarioBinding.bind(knownScenario()),
				rollbackVerificationBinding.bind(
						rollbackReference(true, true),
						verificationReference(true, false, true),
						false
				),
				true,
				true,
				false
		));
		SafetyPolicyDecision verificationRestrictedDecision = gate.evaluate(requirement(
				assess(RuntimeState.CONVERGED, completeEvidence(), false, false),
				scenarioBinding.bind(knownScenario()),
				rollbackVerificationBinding.bind(
						rollbackReference(true, false),
						verificationReference(true, true, true),
						false
				),
				true,
				true,
				false
		));

		assertThat(rollbackRestrictedDecision.scope())
				.isEqualTo(SafetyPolicyScope.RESTRICTED);
		assertThat(verificationRestrictedDecision.scope())
				.isEqualTo(SafetyPolicyScope.RESTRICTED);
	}

	@Test
	void shouldRejectPaymentSafetyActionWithoutPaymentConsistencyVerification() {
		SafetyPolicyDecision decision = gate.evaluate(requirement(
				assess(RuntimeState.CONVERGED, completeEvidence(), false, false),
				scenarioBinding.bind(knownScenario()),
				rollbackVerificationBinding.bind(
						rollbackReference(true, false),
						verificationReference(true, false, false),
						true
				),
				true,
				true,
				true
		));

		assertThat(decision.admitted()).isFalse();
		assertThat(decision.rejectionReason()).isEqualTo(
				SafetyPolicyRejectionReason.MISSING_PAYMENT_CONSISTENCY_VERIFICATION
		);
	}

	@Test
	void shouldRemainSemanticSafetyAdmissionControlOnly() {
		SafetyPolicyDecision decision = gate.evaluate(requirement(
				assess(RuntimeState.CONVERGED, completeEvidence(), false, false),
				scenarioBinding.bind(knownScenario()),
				rollbackVerificationBinding.bind(
						rollbackReference(true, false),
						verificationReference(true, false, true),
						false
				),
				true,
				true,
				false
		));

		assertThat(decision.admitted()).isTrue();
		assertThat(decision.semanticSafetyAdmissionOnly()).isTrue();
		assertThat(decision.executionPermission()).isFalse();
	}

	@Test
	void shouldRejectNullRequirement() {
		assertThatThrownBy(() -> gate.evaluate(null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("requirement must not be null");
	}

	private SafetyPolicyRequirement requirement(
			ReliabilityAssessmentResult assessmentResult,
			ScenarioBindingDecision scenarioBindingDecision,
			RollbackVerificationBindingDecision rollbackVerificationBindingDecision,
			boolean approvalProvided,
			boolean explicitApprovalProvided,
			boolean paymentSafetyAction
	) {
		ReliabilityRiskClassification riskClassification =
				riskClassifier.classify(assessmentResult);
		HumanApprovalDecision humanApprovalDecision = humanApprovalPolicy.evaluate(
				assessmentResult,
				riskClassification
		);
		return new SafetyPolicyRequirement(
				assessmentResult,
				riskClassification,
				humanApprovalDecision,
				scenarioBindingDecision,
				rollbackVerificationBindingDecision,
				approvalProvided,
				explicitApprovalProvided,
				paymentSafetyAction
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
