package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public class ExecutionBoundary {

	public ExecutionBoundaryDecision evaluate(
			ExecutionRequirement requirement
	) {
		Objects.requireNonNull(requirement, "requirement must not be null");

		if (!requirement.actionAdmissionDecision().admitted()) {
			return rejected(
					requirement,
					ExecutionBoundaryRejectionReason.ACTION_ADMISSION_NOT_ACCEPTED
			);
		}
		if (!requirement.explicitExecutionAuthorized()) {
			return rejected(
					requirement,
					ExecutionBoundaryRejectionReason
							.EXPLICIT_EXECUTION_AUTHORIZATION_MISSING
			);
		}
		if (requirement.actionAdmissionDecision().requirement().approvalRequired()
				&& !requirement.approvalCompleted()) {
			return rejected(
					requirement,
					ExecutionBoundaryRejectionReason.APPROVAL_NOT_COMPLETED
			);
		}
		if (requirement.actionAdmissionDecision().requirement().rollbackReviewRequired()
				&& !requirement.rollbackReviewCompleted()) {
			return rejected(
					requirement,
					ExecutionBoundaryRejectionReason.ROLLBACK_REVIEW_NOT_COMPLETED
			);
		}
		if (requirement.actionAdmissionDecision().requirement().verificationReviewRequired()
				&& !requirement.verificationReviewCompleted()) {
			return rejected(
					requirement,
					ExecutionBoundaryRejectionReason
							.VERIFICATION_REVIEW_NOT_COMPLETED
			);
		}
		if (requirement.assessmentResult().evidenceCorrelation().paymentSafetyUncertain()) {
			return rejected(
					requirement,
					ExecutionBoundaryRejectionReason.PAYMENT_SAFETY_UNCERTAINTY
			);
		}
		if (requirement.assessmentResult().evidenceCorrelation().contradictoryEvidence()) {
			return rejected(
					requirement,
					ExecutionBoundaryRejectionReason.CONTRADICTORY_EVIDENCE
			);
		}
		if (requirement.critical()
				&& (!requirement.explicitExecutionAuthorized()
						|| !requirement.approvalCompleted()
						|| !requirement.rollbackReviewCompleted()
						|| !requirement.verificationReviewCompleted())) {
			return rejected(
					requirement,
					ExecutionBoundaryRejectionReason
							.CRITICAL_EXECUTION_REQUIREMENTS_NOT_SATISFIED
			);
		}

		ActionAdmissionScope admissionScope =
				requirement.actionAdmissionDecision().scope();
		return new ExecutionBoundaryDecision(
				true,
				admissionScope == ActionAdmissionScope.RESTRICTED_CANDIDATE
						? ExecutionScope.RESTRICTED_ELIGIBLE
						: ExecutionScope.ELIGIBLE,
				requirement,
				null
		);
	}

	private ExecutionBoundaryDecision rejected(
			ExecutionRequirement requirement,
			ExecutionBoundaryRejectionReason rejectionReason
	) {
		return new ExecutionBoundaryDecision(
				false,
				ExecutionScope.NONE,
				requirement,
				rejectionReason
		);
	}
}
