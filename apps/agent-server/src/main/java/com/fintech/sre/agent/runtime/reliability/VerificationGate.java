package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public class VerificationGate {

	public VerificationGateDecision evaluate(
			VerificationRequirement requirement
	) {
		Objects.requireNonNull(requirement, "requirement must not be null");

		EvidenceCorrelation correlation = requirement.correlation();
		if (!correlation.verificationEvidencePresent()) {
			return VerificationGateDecision.rejected(
					requirement,
					VerificationGateRejectionReason.MISSING_VERIFICATION_EVIDENCE
			);
		}
		if (correlation.paymentSafetyUncertain()) {
			return VerificationGateDecision.rejected(
					requirement,
					VerificationGateRejectionReason.PAYMENT_SAFETY_UNCERTAINTY
			);
		}
		if (correlation.contradictoryEvidence()) {
			return VerificationGateDecision.rejected(
					requirement,
					VerificationGateRejectionReason.CONTRADICTORY_EVIDENCE
			);
		}
		if (requirement.type() == VerificationRequirementType.CONVERGED
				&& correlation.completeness() != EvidenceCompleteness.COMPLETE) {
			return VerificationGateDecision.rejected(
					requirement,
					VerificationGateRejectionReason.INSUFFICIENT_EVIDENCE_COMPLETENESS
			);
		}
		if (requirement.type() == VerificationRequirementType.VERIFIED
				&& correlation.completeness() == EvidenceCompleteness.ABSENT) {
			return VerificationGateDecision.rejected(
					requirement,
					VerificationGateRejectionReason.INSUFFICIENT_EVIDENCE_COMPLETENESS
			);
		}

		return VerificationGateDecision.admitted(requirement);
	}

	public boolean executionTrigger() {
		return false;
	}
}
