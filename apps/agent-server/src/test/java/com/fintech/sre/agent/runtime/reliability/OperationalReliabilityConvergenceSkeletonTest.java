package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

class OperationalReliabilityConvergenceSkeletonTest {

	@Test
	void shouldTreatVerifiedAsDifferentFromConverged() {
		ConvergenceAssessment assessment = assessment(
				RuntimeState.VERIFIED,
				true,
				false,
				false,
				Duration.ofMinutes(5),
				Duration.ofMinutes(5)
		);

		ConvergenceDecision decision = ConvergenceDecision.evaluate(assessment);

		assertThat(assessment.runtimeState()).isEqualTo(RuntimeState.VERIFIED);
		assertThat(decision.converged()).isTrue();
		assertThat(decision.status()).isEqualTo(ConvergenceStatus.CONVERGED);
	}

	@Test
	void shouldRejectVerificationOnlyWithoutTemporalStability() {
		ConvergenceDecision decision = ConvergenceDecision.evaluate(
				assessment(
						RuntimeState.VERIFIED,
						true,
						false,
						false,
						Duration.ofMinutes(10),
						Duration.ofMinutes(2)
				)
		);

		assertThat(decision.converged()).isFalse();
		assertThat(decision.rejectionReason())
				.isEqualTo(
						ConvergenceRejectionReason.TEMPORAL_STABILITY_NOT_SATISFIED
				);
	}

	@Test
	void shouldRejectConvergenceWhilePropagationIsActive() {
		ConvergenceDecision decision = ConvergenceDecision.evaluate(
				assessment(
						RuntimeState.VERIFIED,
						true,
						true,
						false,
						Duration.ofMinutes(5),
						Duration.ofMinutes(5)
				)
		);

		assertThat(decision.converged()).isFalse();
		assertThat(decision.rejectionReason())
				.isEqualTo(ConvergenceRejectionReason.PROPAGATION_STILL_ACTIVE);
	}

	@Test
	void shouldRejectConvergenceWithoutStabilizationWindowAfterRollback() {
		ConvergenceDecision decision = ConvergenceDecision.evaluate(
				assessment(
						RuntimeState.VERIFIED,
						true,
						false,
						true,
						Duration.ofMinutes(15),
						Duration.ofMinutes(5)
				)
		);

		assertThat(decision.converged()).isFalse();
		assertThat(decision.rejectionReason())
				.isEqualTo(
						ConvergenceRejectionReason.STABILIZATION_WINDOW_NOT_SATISFIED
				);
	}

	@Test
	void shouldRejectConvergenceWhenEvidenceIsContradictory() {
		ConvergenceDecision decision = ConvergenceDecision.evaluate(
				assessment(
						RuntimeState.VERIFIED,
						true,
						false,
						false,
						Duration.ofMinutes(5),
						Duration.ofMinutes(5),
						true
				)
		);

		assertThat(decision.converged()).isFalse();
		assertThat(decision.rejectionReason())
				.isEqualTo(ConvergenceRejectionReason.CONTRADICTORY_EVIDENCE);
	}

	@Test
	void shouldRequireVerificationAdmissionBeforeConvergence() {
		ConvergenceDecision decision = ConvergenceDecision.evaluate(
				assessment(
						RuntimeState.VERIFIED,
						false,
						false,
						false,
						Duration.ofMinutes(5),
						Duration.ofMinutes(5)
				)
		);

		assertThat(decision.converged()).isFalse();
		assertThat(decision.rejectionReason())
				.isEqualTo(ConvergenceRejectionReason.VERIFICATION_NOT_ADMITTED);
	}

	@Test
	void shouldTreatConvergenceAsSemanticTerminalButNotImmutableTruth() {
		assertThat(ConvergenceStatus.CONVERGED.terminalSemantic()).isTrue();
		assertThat(assessment(
				RuntimeState.CONVERGED,
				true,
				false,
				false,
				Duration.ofMinutes(5),
				Duration.ofMinutes(5)
		).canDegradeAfterConvergence()).isTrue();
	}

	@Test
	void shouldRejectNullAssessment() {
		assertThatThrownBy(() -> ConvergenceDecision.evaluate(null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("assessment must not be null");
	}

	@Test
	void shouldValidatePositiveConvergenceWindow() {
		assertThatThrownBy(() -> new ConvergenceWindow(
				Duration.ZERO,
				Duration.ofMinutes(1)
		))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("requiredStabilization must be positive");
	}

	private ConvergenceAssessment assessment(
			RuntimeState runtimeState,
			boolean verificationAdmitted,
			boolean propagationActive,
			boolean rollbackRecentlyApplied,
			Duration required,
			Duration observed
	) {
		return assessment(
				runtimeState,
				verificationAdmitted,
				propagationActive,
				rollbackRecentlyApplied,
				required,
				observed,
				false
		);
	}

	private ConvergenceAssessment assessment(
			RuntimeState runtimeState,
			boolean verificationAdmitted,
			boolean propagationActive,
			boolean rollbackRecentlyApplied,
			Duration required,
			Duration observed,
			boolean contradictoryEvidence
	) {
		EvidenceCorrelation correlation = EvidenceCorrelation.correlate(
				List.of(
						signal(EvidenceSignalType.METRIC, "metric-1"),
						signal(EvidenceSignalType.LOG, "log-1"),
						signal(EvidenceSignalType.TRACE, "trace-1"),
						signal(EvidenceSignalType.TIMELINE, "timeline-1"),
						signal(EvidenceSignalType.VERIFICATION, "verification-1"),
						signal(EvidenceSignalType.PAYMENT_SAFETY, "payment-1")
				),
				contradictoryEvidence
		);
		VerificationRequirement requirement = new VerificationRequirement(
				VerificationRequirementType.VERIFIED,
				correlation
		);
		VerificationGateDecision gateDecision = verificationAdmitted
				? VerificationGateDecision.admitted(requirement)
				: VerificationGateDecision.rejected(
						requirement,
						VerificationGateRejectionReason.MISSING_VERIFICATION_EVIDENCE
				);

		return new ConvergenceAssessment(
				runtimeState,
				new ConvergenceEvidence(correlation, gateDecision),
				new ConvergenceWindow(required, observed),
				PropagationSignal.CROSS_SERVICE,
				propagationActive,
				rollbackRecentlyApplied
		);
	}

	private EvidenceSignal signal(EvidenceSignalType type, String signalId) {
		return new EvidenceSignal(type, signalId, "summary-" + signalId);
	}
}
