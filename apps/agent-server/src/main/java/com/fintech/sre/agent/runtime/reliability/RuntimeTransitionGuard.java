package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public class RuntimeTransitionGuard {

	public RuntimeTransitionDecision evaluate(
			RuntimeState from,
			RuntimeState to,
			ReliabilityAssessment assessment
	) {
		Objects.requireNonNull(from, "from must not be null");
		Objects.requireNonNull(to, "to must not be null");
		Objects.requireNonNull(assessment, "assessment must not be null");

		if (from == RuntimeState.FAILED) {
			return RuntimeTransitionDecision.rejected(
					RuntimeTransitionRejectionReason.FAILED_IS_TERMINAL
			);
		}
		if (!from.canTransitionTo(to)) {
			return RuntimeTransitionDecision.rejected(
					RuntimeTransitionRejectionReason.GENERIC_TRANSITION_NOT_ALLOWED
			);
		}
		if (to == RuntimeState.ROLLBACKING
				&& !rollbackAvailable(assessment.rollbackResult())) {
			return RuntimeTransitionDecision.rejected(
					RuntimeTransitionRejectionReason.ROLLBACK_UNAVAILABLE
			);
		}
		if (from == RuntimeState.UNKNOWN && to == RuntimeState.CONVERGED) {
			return RuntimeTransitionDecision.rejected(
					RuntimeTransitionRejectionReason.UNKNOWN_CANNOT_CONVERGE_DIRECTLY
			);
		}
		if (from == RuntimeState.PROPAGATING && to == RuntimeState.CONVERGED) {
			return RuntimeTransitionDecision.rejected(
					RuntimeTransitionRejectionReason.PROPAGATING_REQUIRES_VERIFICATION
			);
		}
		if (from == RuntimeState.ROLLBACKING
				&& to != RuntimeState.VERIFIED
				&& to != RuntimeState.FAILED) {
			return RuntimeTransitionDecision.rejected(
					RuntimeTransitionRejectionReason.ROLLBACK_REQUIRES_VERIFIED_OR_FAILED
			);
		}
		if (to == RuntimeState.CONVERGED && from != RuntimeState.VERIFIED) {
			return RuntimeTransitionDecision.rejected(
					RuntimeTransitionRejectionReason.CONVERGENCE_REQUIRES_VERIFIED_STATE
			);
		}
		if (to == RuntimeState.CONVERGED
				&& assessment.operationalUncertainty()
						== OperationalUncertainty.CRITICAL) {
			return RuntimeTransitionDecision.rejected(
					RuntimeTransitionRejectionReason.PAYMENT_SAFETY_UNCERTAINTY
			);
		}

		return RuntimeTransitionDecision.allowed(null);
	}

	private boolean rollbackAvailable(RollbackResult rollbackResult) {
		return rollbackResult != null && rollbackResult != RollbackResult.NOT_ATTEMPTED;
	}
}
