package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record ConvergenceEvidence(
		EvidenceCorrelation correlation,
		VerificationGateDecision verificationGateDecision
) {
	public ConvergenceEvidence {
		Objects.requireNonNull(correlation, "correlation must not be null");
		Objects.requireNonNull(
				verificationGateDecision,
				"verificationGateDecision must not be null"
		);
	}

	public boolean verificationAdmitted() {
		return verificationGateDecision.admitted();
	}

	public boolean contradictory() {
		return correlation.contradictoryEvidence();
	}
}
