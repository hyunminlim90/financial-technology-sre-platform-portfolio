package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record EvidenceCorrelationDecision(
		boolean supportsConvergedAssessment,
		EvidenceCorrelation correlation,
		EvidenceCorrelationRejectionReason rejectionReason
) {
	public EvidenceCorrelationDecision {
		Objects.requireNonNull(correlation, "correlation must not be null");
		if (!supportsConvergedAssessment && rejectionReason == null) {
			throw new IllegalArgumentException(
					"rejected correlation decision requires rejection reason"
			);
		}
		if (supportsConvergedAssessment && rejectionReason != null) {
			throw new IllegalArgumentException(
					"accepted correlation decision must not contain rejection reason"
			);
		}
	}

	public static EvidenceCorrelationDecision evaluateForConverged(
			EvidenceCorrelation correlation
	) {
		Objects.requireNonNull(correlation, "correlation must not be null");

		if (!correlation.verificationEvidencePresent()) {
			return new EvidenceCorrelationDecision(
					false,
					correlation,
					EvidenceCorrelationRejectionReason.MISSING_VERIFICATION_EVIDENCE
			);
		}
		if (correlation.paymentSafetyUncertain()) {
			return new EvidenceCorrelationDecision(
					false,
					correlation,
					EvidenceCorrelationRejectionReason.PAYMENT_SAFETY_EVIDENCE_MISSING
			);
		}
		if (correlation.contradictoryEvidence()) {
			return new EvidenceCorrelationDecision(
					false,
					correlation,
					EvidenceCorrelationRejectionReason.HIGH_RISK_UNCERTAINTY
			);
		}
		return new EvidenceCorrelationDecision(true, correlation, null);
	}
}
