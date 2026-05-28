package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public class PostExecutionConvergence {

	public PostExecutionConvergenceDecision assess(
			PostExecutionConvergenceRequirement requirement
	) {
		Objects.requireNonNull(requirement, "requirement must not be null");

		PostExecutionVerificationRequirement verificationRequirement =
				requirement.verificationRequirement();

		if (verificationRequirement.executorResponse().status() == ExecutorStatus.UNKNOWN) {
			return rejected(
					requirement,
					PostExecutionConvergenceRejectionReason.UNKNOWN_EXECUTOR_RESPONSE
			);
		}
		if (!requirement.verificationDecision().verified()) {
			return incomplete(
					requirement,
					PostExecutionConvergenceRejectionReason.VERIFICATION_NOT_SUFFICIENT
			);
		}
		if (!requirement.convergenceWindow().satisfied()) {
			return rejected(
					requirement,
					PostExecutionConvergenceRejectionReason
							.STABILIZATION_WINDOW_NOT_SATISFIED
			);
		}
		if (requirement.propagationActive()) {
			return rejected(
					requirement,
					PostExecutionConvergenceRejectionReason.PROPAGATION_STILL_ACTIVE
			);
		}
		if (verificationRequirement.evidenceCorrelation().contradictoryEvidence()) {
			return rejected(
					requirement,
					PostExecutionConvergenceRejectionReason
							.CONTRADICTORY_POST_EXECUTION_EVIDENCE
			);
		}
		if (verificationRequirement.paymentImpactingExecution()
				&& !verificationRequirement.paymentConsistencyVerified()) {
			return rejected(
					requirement,
					PostExecutionConvergenceRejectionReason
							.MISSING_PAYMENT_CONSISTENCY_VERIFICATION
			);
		}
		if (verificationRequirement.rollbackTriggeredExecution()
				&& !verificationRequirement.rollbackVerified()) {
			return rejected(
					requirement,
					PostExecutionConvergenceRejectionReason
							.ROLLBACK_VERIFICATION_REQUIRED
			);
		}

		return new PostExecutionConvergenceDecision(
				PostExecutionConvergenceStatus.CONVERGED,
				requirement,
				null
		);
	}

	private PostExecutionConvergenceDecision rejected(
			PostExecutionConvergenceRequirement requirement,
			PostExecutionConvergenceRejectionReason rejectionReason
	) {
		return new PostExecutionConvergenceDecision(
				PostExecutionConvergenceStatus.REJECTED,
				requirement,
				rejectionReason
		);
	}

	private PostExecutionConvergenceDecision incomplete(
			PostExecutionConvergenceRequirement requirement,
			PostExecutionConvergenceRejectionReason rejectionReason
	) {
		return new PostExecutionConvergenceDecision(
				PostExecutionConvergenceStatus.INCOMPLETE,
				requirement,
				rejectionReason
		);
	}
}
