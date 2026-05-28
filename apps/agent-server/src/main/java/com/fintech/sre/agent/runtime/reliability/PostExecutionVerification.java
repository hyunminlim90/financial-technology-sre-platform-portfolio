package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public class PostExecutionVerification {

	public PostExecutionVerificationDecision verify(
			PostExecutionVerificationRequirement requirement
	) {
		Objects.requireNonNull(requirement, "requirement must not be null");

		if (requirement.executorResponse().status() == ExecutorStatus.UNKNOWN) {
			return rejected(
					requirement,
					PostExecutionVerificationRejectionReason.EXECUTOR_RESPONSE_UNKNOWN
			);
		}
		if (requirement.executorResponse().status() == ExecutorStatus.SUCCESS
				&& !requirement.evidenceCorrelation().verificationEvidencePresent()) {
			return incomplete(
					requirement,
					PostExecutionVerificationRejectionReason
							.EXECUTION_ACKNOWLEDGEMENT_ONLY
			);
		}
		if (!requirement.evidenceCorrelation().verificationEvidencePresent()) {
			return rejected(
					requirement,
					PostExecutionVerificationRejectionReason
							.MISSING_VERIFICATION_EVIDENCE
			);
		}
		if (requirement.evidenceCorrelation().contradictoryEvidence()) {
			return rejected(
					requirement,
					PostExecutionVerificationRejectionReason
							.CONTRADICTORY_POST_EXECUTION_EVIDENCE
			);
		}
		if (requirement.paymentImpactingExecution()
				&& !requirement.paymentConsistencyVerified()) {
			return rejected(
					requirement,
					PostExecutionVerificationRejectionReason
							.MISSING_PAYMENT_CONSISTENCY_VERIFICATION
			);
		}
		if (requirement.rollbackTriggeredExecution()
				&& !requirement.rollbackVerified()) {
			return rejected(
					requirement,
					PostExecutionVerificationRejectionReason
							.ROLLBACK_VERIFICATION_REQUIRED
			);
		}
		if (requirement.verificationIncomplete()) {
			return incomplete(
					requirement,
					PostExecutionVerificationRejectionReason
							.INCOMPLETE_VERIFICATION_EVIDENCE
			);
		}

		return new PostExecutionVerificationDecision(
				PostExecutionVerificationStatus.VERIFIED,
				requirement,
				null
		);
	}

	private PostExecutionVerificationDecision rejected(
			PostExecutionVerificationRequirement requirement,
			PostExecutionVerificationRejectionReason rejectionReason
	) {
		return new PostExecutionVerificationDecision(
				PostExecutionVerificationStatus.REJECTED,
				requirement,
				rejectionReason
		);
	}

	private PostExecutionVerificationDecision incomplete(
			PostExecutionVerificationRequirement requirement,
			PostExecutionVerificationRejectionReason rejectionReason
	) {
		return new PostExecutionVerificationDecision(
				PostExecutionVerificationStatus.INCOMPLETE,
				requirement,
				rejectionReason
		);
	}
}
