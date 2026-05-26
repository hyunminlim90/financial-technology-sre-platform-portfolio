package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

class OperationalReliabilityVerificationGateSkeletonTest {

	private final VerificationGate gate = new VerificationGate();

	@Test
	void shouldRejectVerifiedWithoutVerificationEvidence() {
		VerificationRequirement requirement = requirement(
				VerificationRequirementType.VERIFIED,
				correlation(
						List.of(
								signal(EvidenceSignalType.METRIC, "metric-1"),
								signal(EvidenceSignalType.LOG, "log-1"),
								signal(EvidenceSignalType.PAYMENT_SAFETY, "payment-1")
						),
						false
				)
		);

		VerificationGateDecision decision = gate.evaluate(requirement);

		assertThat(decision.admitted()).isFalse();
		assertThat(decision.rejectionReason())
				.isEqualTo(
						VerificationGateRejectionReason.MISSING_VERIFICATION_EVIDENCE
				);
	}

	@Test
	void shouldRejectConvergedWithoutVerificationEvidence() {
		VerificationRequirement requirement = requirement(
				VerificationRequirementType.CONVERGED,
				correlation(
						List.of(
								signal(EvidenceSignalType.METRIC, "metric-1"),
								signal(EvidenceSignalType.LOG, "log-1"),
								signal(EvidenceSignalType.TRACE, "trace-1"),
								signal(EvidenceSignalType.TIMELINE, "timeline-1"),
								signal(EvidenceSignalType.PAYMENT_SAFETY, "payment-1")
						),
						false
				)
		);

		VerificationGateDecision decision = gate.evaluate(requirement);

		assertThat(decision.admitted()).isFalse();
		assertThat(decision.rejectionReason())
				.isEqualTo(
						VerificationGateRejectionReason.MISSING_VERIFICATION_EVIDENCE
				);
	}

	@Test
	void shouldRejectWhenPaymentSafetyUncertaintyExists() {
		VerificationRequirement requirement = requirement(
				VerificationRequirementType.VERIFIED,
				correlation(
						List.of(
								signal(EvidenceSignalType.METRIC, "metric-1"),
								signal(EvidenceSignalType.LOG, "log-1"),
								signal(EvidenceSignalType.VERIFICATION, "verification-1")
						),
						false
				)
		);

		VerificationGateDecision decision = gate.evaluate(requirement);

		assertThat(decision.admitted()).isFalse();
		assertThat(decision.rejectionReason())
				.isEqualTo(
						VerificationGateRejectionReason.PAYMENT_SAFETY_UNCERTAINTY
				);
	}

	@Test
	void shouldRejectWhenEvidenceIsContradictory() {
		VerificationRequirement requirement = requirement(
				VerificationRequirementType.VERIFIED,
				correlation(
						List.of(
								signal(EvidenceSignalType.METRIC, "metric-1"),
								signal(EvidenceSignalType.LOG, "log-1"),
								signal(EvidenceSignalType.TRACE, "trace-1"),
								signal(EvidenceSignalType.TIMELINE, "timeline-1"),
								signal(EvidenceSignalType.VERIFICATION, "verification-1"),
								signal(EvidenceSignalType.PAYMENT_SAFETY, "payment-1")
						),
						true
				)
		);

		VerificationGateDecision decision = gate.evaluate(requirement);

		assertThat(decision.admitted()).isFalse();
		assertThat(decision.rejectionReason())
				.isEqualTo(VerificationGateRejectionReason.CONTRADICTORY_EVIDENCE);
	}

	@Test
	void shouldAllowVerifiedCandidateWithPartialEvidence() {
		VerificationRequirement requirement = requirement(
				VerificationRequirementType.VERIFIED,
				correlation(
						List.of(
								signal(EvidenceSignalType.METRIC, "metric-1"),
								signal(EvidenceSignalType.LOG, "log-1"),
								signal(EvidenceSignalType.VERIFICATION, "verification-1"),
								signal(EvidenceSignalType.PAYMENT_SAFETY, "payment-1")
						),
						false
				)
		);

		VerificationGateDecision decision = gate.evaluate(requirement);

		assertThat(requirement.correlation().completeness())
				.isEqualTo(EvidenceCompleteness.PARTIAL);
		assertThat(decision.admitted()).isTrue();
	}

	@Test
	void shouldRejectConvergedCandidateWithPartialEvidence() {
		VerificationRequirement requirement = requirement(
				VerificationRequirementType.CONVERGED,
				correlation(
						List.of(
								signal(EvidenceSignalType.METRIC, "metric-1"),
								signal(EvidenceSignalType.LOG, "log-1"),
								signal(EvidenceSignalType.VERIFICATION, "verification-1"),
								signal(EvidenceSignalType.PAYMENT_SAFETY, "payment-1")
						),
						false
				)
		);

		VerificationGateDecision decision = gate.evaluate(requirement);

		assertThat(decision.admitted()).isFalse();
		assertThat(decision.rejectionReason())
				.isEqualTo(
						VerificationGateRejectionReason.INSUFFICIENT_EVIDENCE_COMPLETENESS
				);
	}

	@Test
	void shouldAllowConvergedCandidateOnlyWithCompleteEvidenceAndNoPaymentUncertainty() {
		VerificationRequirement requirement = requirement(
				VerificationRequirementType.CONVERGED,
				correlation(
						List.of(
								signal(EvidenceSignalType.METRIC, "metric-1"),
								signal(EvidenceSignalType.LOG, "log-1"),
								signal(EvidenceSignalType.TRACE, "trace-1"),
								signal(EvidenceSignalType.TIMELINE, "timeline-1"),
								signal(EvidenceSignalType.VERIFICATION, "verification-1"),
								signal(EvidenceSignalType.PAYMENT_SAFETY, "payment-1")
						),
						false
				)
		);

		VerificationGateDecision decision = gate.evaluate(requirement);

		assertThat(decision.admitted()).isTrue();
		assertThat(decision.executionTrigger()).isFalse();
	}

	@Test
	void shouldRemainSemanticAdmissionControlOnly() {
		assertThat(gate.executionTrigger()).isFalse();
	}

	@Test
	void shouldRejectNullRequirement() {
		assertThatThrownBy(() -> gate.evaluate(null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("requirement must not be null");
	}

	private VerificationRequirement requirement(
			VerificationRequirementType type,
			EvidenceCorrelation correlation
	) {
		return new VerificationRequirement(type, correlation);
	}

	private EvidenceCorrelation correlation(
			List<EvidenceSignal> signals,
			boolean contradictoryEvidence
	) {
		return EvidenceCorrelation.correlate(signals, contradictoryEvidence);
	}

	private EvidenceSignal signal(EvidenceSignalType type, String signalId) {
		return new EvidenceSignal(type, signalId, "summary-" + signalId);
	}
}
