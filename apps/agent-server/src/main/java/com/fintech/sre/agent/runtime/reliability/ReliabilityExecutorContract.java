package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public class ReliabilityExecutorContract {

	public ExecutionPlan plan(
			ExecutionIntent intent
	) {
		Objects.requireNonNull(intent, "intent must not be null");

		if (!intent.executionBoundaryDecision().executionEligible()) {
			return rejected(
					intent,
					ExecutionPlanRejectionReason.EXECUTION_NOT_ELIGIBLE
			);
		}
		if (!intent.explicitExecutionAuthorized()) {
			return rejected(
					intent,
					ExecutionPlanRejectionReason
							.MISSING_EXPLICIT_EXECUTION_AUTHORIZATION
			);
		}
		if (intent.rollbackPlanReference() == null
				|| intent.rollbackPlanReference().isBlank()) {
			return rejected(
					intent,
					ExecutionPlanRejectionReason.MISSING_ROLLBACK_PLAN_REFERENCE
			);
		}
		if (intent.verificationPlanReference() == null
				|| intent.verificationPlanReference().isBlank()) {
			return rejected(
					intent,
					ExecutionPlanRejectionReason.MISSING_VERIFICATION_PLAN_REFERENCE
			);
		}
		if (intent.paymentImpacting()
				&& !intent.paymentConsistencyVerificationIncluded()) {
			return rejected(
					intent,
					ExecutionPlanRejectionReason
							.MISSING_PAYMENT_CONSISTENCY_VERIFICATION
			);
		}
		if (intent.aiOnlyApproved()) {
			return rejected(
					intent,
					ExecutionPlanRejectionReason.AI_ONLY_APPROVAL_PROHIBITED
			);
		}

		return new ExecutionPlan(
				ExecutionPlanStatus.STRUCTURED,
				intent,
				intent.rollbackPlanReference(),
				intent.verificationPlanReference(),
				null
		);
	}

	private ExecutionPlan rejected(
			ExecutionIntent intent,
			ExecutionPlanRejectionReason rejectionReason
	) {
		return new ExecutionPlan(
				ExecutionPlanStatus.REJECTED,
				intent,
				intent.rollbackPlanReference(),
				intent.verificationPlanReference(),
				rejectionReason
		);
	}
}
