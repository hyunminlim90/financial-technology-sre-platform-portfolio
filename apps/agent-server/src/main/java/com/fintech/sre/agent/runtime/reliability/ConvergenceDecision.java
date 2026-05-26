package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record ConvergenceDecision(
		boolean converged,
		ConvergenceStatus status,
		ConvergenceAssessment assessment,
		ConvergenceRejectionReason rejectionReason
) {
	public ConvergenceDecision {
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(assessment, "assessment must not be null");
		if (converged && rejectionReason != null) {
			throw new IllegalArgumentException(
					"converged decision must not contain rejection reason"
			);
		}
		if (!converged && rejectionReason == null
				&& status == ConvergenceStatus.REJECTED) {
			throw new IllegalArgumentException(
					"rejected convergence decision requires rejection reason"
			);
		}
	}

	public static ConvergenceDecision evaluate(
			ConvergenceAssessment assessment
	) {
		Objects.requireNonNull(assessment, "assessment must not be null");

		if (!assessment.evidence().verificationAdmitted()) {
			return rejected(
					assessment,
					ConvergenceRejectionReason.VERIFICATION_NOT_ADMITTED
			);
		}
		if (assessment.runtimeState() != RuntimeState.VERIFIED) {
			return rejected(
					assessment,
					ConvergenceRejectionReason.VERIFICATION_ONLY_NOT_SUFFICIENT
			);
		}
		if (assessment.propagationActive()) {
			return rejected(
					assessment,
					ConvergenceRejectionReason.PROPAGATION_STILL_ACTIVE
			);
		}
		if (assessment.evidence().contradictory()) {
			return rejected(
					assessment,
					ConvergenceRejectionReason.CONTRADICTORY_EVIDENCE
			);
		}
		if (assessment.rollbackRecentlyApplied()
				&& !assessment.convergenceWindow().satisfied()) {
			return rejected(
					assessment,
					ConvergenceRejectionReason.STABILIZATION_WINDOW_NOT_SATISFIED
			);
		}
		if (!assessment.temporalStabilitySatisfied()) {
			return rejected(
					assessment,
					ConvergenceRejectionReason.TEMPORAL_STABILITY_NOT_SATISFIED
			);
		}

		return new ConvergenceDecision(
				true,
				ConvergenceStatus.CONVERGED,
				assessment,
				null
		);
	}

	private static ConvergenceDecision rejected(
			ConvergenceAssessment assessment,
			ConvergenceRejectionReason rejectionReason
	) {
		return new ConvergenceDecision(
				false,
				ConvergenceStatus.REJECTED,
				assessment,
				rejectionReason
		);
	}
}
