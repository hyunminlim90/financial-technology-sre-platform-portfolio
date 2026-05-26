package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

class OperationalReliabilityAssessmentOrchestratorSkeletonTest {

	private final ReliabilityAssessmentOrchestrator orchestrator =
			new ReliabilityAssessmentOrchestrator(new VerificationGate());

	@Test
	void shouldAssessInFixedSemanticOrder() {
		ReliabilityAssessmentResult result = orchestrator.assess(input(
				RuntimeState.VERIFIED,
				completeEvidence(),
				false,
				false,
				List.of()
		));

		assertThat(result.stages()).containsExactly(
				ReliabilityAssessmentStage.EVIDENCE_CORRELATION,
				ReliabilityAssessmentStage.VERIFICATION_GATE,
				ReliabilityAssessmentStage.CONVERGENCE_ASSESSMENT,
				ReliabilityAssessmentStage.REGRESSION_ASSESSMENT
		);
		assertThat(result.verificationGateDecision().admitted()).isTrue();
		assertThat(result.convergenceDecision().converged()).isTrue();
	}

	@Test
	void shouldPrioritizeContradictoryEvidenceAsRegressionBeforeConvergenceOutcome() {
		ReliabilityAssessmentResult result = orchestrator.assess(input(
				RuntimeState.CONVERGED,
				completeEvidence(),
				true,
				false,
				List.of()
		));

		assertThat(result.regressionDecision().regressionDetected()).isTrue();
		assertThat(result.regressionDecision().severity())
				.isEqualTo(RegressionSeverity.HIGH);
		assertThat(result.rejectionReason())
				.isEqualTo(
						ReliabilityAssessmentRejectionReason
								.REGRESSION_PRIORITIZED_OVER_CONVERGENCE
				);
	}

	@Test
	void shouldEscalatePaymentSafetyUncertaintyToHighOrAbove() {
		ReliabilityAssessmentResult result = orchestrator.assess(input(
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
		));

		assertThat(result.evidenceCorrelation().paymentSafetyUncertain()).isTrue();
		assertThat(result.overallRisk()).isIn(
				OperationalUncertainty.HIGH,
				OperationalUncertainty.CRITICAL
		);
	}

	@Test
	void shouldKeepFailedStateTerminal() {
		ReliabilityAssessmentResult result = orchestrator.assess(input(
				RuntimeState.FAILED,
				completeEvidence(),
				false,
				false,
				List.of()
		));

		assertThat(result.terminal()).isTrue();
		assertThat(result.rejectionReason())
				.isEqualTo(
						ReliabilityAssessmentRejectionReason.FAILED_STATE_TERMINAL
				);
		assertThat(result.stages()).containsExactly(
				ReliabilityAssessmentStage.EVIDENCE_CORRELATION
		);
		assertThat(result.verificationGateDecision()).isNull();
		assertThat(result.convergenceDecision()).isNull();
		assertThat(result.regressionDecision()).isNull();
	}

	@Test
	void shouldRemainReadOnlySemanticAssessmentOnly() {
		ReliabilityAssessmentResult result = orchestrator.assess(input(
				RuntimeState.VERIFIED,
				completeEvidence(),
				false,
				false,
				List.of()
		));

		assertThat(result.semanticOnly()).isTrue();
		assertThat(result.executionTrigger()).isFalse();
	}

	@Test
	void shouldRejectNullVerificationGateDependency() {
		assertThatThrownBy(() -> new ReliabilityAssessmentOrchestrator(null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("verificationGate must not be null");
	}

	@Test
	void shouldRejectNullInput() {
		assertThatThrownBy(() -> orchestrator.assess(null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("input must not be null");
	}

	private ReliabilityAssessmentInput input(
			RuntimeState runtimeState,
			List<EvidenceSignal> evidenceSignals,
			boolean contradictoryEvidence,
			boolean propagationActive,
			List<RegressionSignal> regressionSignals
	) {
		return new ReliabilityAssessmentInput(
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
