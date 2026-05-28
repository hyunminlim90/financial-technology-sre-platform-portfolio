package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public class ExecutionReadinessGate {

	public ExecutionReadinessDecision evaluate(
			ExecutionReadinessRequirement requirement
	) {
		Objects.requireNonNull(requirement, "requirement must not be null");

		if (!requirement.executionBoundaryDecision().executionEligible()) {
			return rejected(
					requirement,
					ExecutionReadinessRejectionReason.EXECUTION_BOUNDARY_REJECTED
			);
		}
		if (requirement.executionPlan().status() == ExecutionPlanStatus.REJECTED) {
			return rejected(
					requirement,
					ExecutionReadinessRejectionReason.EXECUTION_PLAN_REJECTED
			);
		}
		if (requirement.executionAuditDecision().integrity()
				== ExecutionAuditIntegrity.INCOMPLETE) {
			return rejected(
					requirement,
					ExecutionReadinessRejectionReason.AUDIT_INTEGRITY_INCOMPLETE
			);
		}
		if (requirement.paymentImpacting()
				&& !requirement.executionPlan()
						.intent()
						.paymentConsistencyVerificationIncluded()) {
			return rejected(
					requirement,
					ExecutionReadinessRejectionReason
							.MISSING_PAYMENT_CONSISTENCY_VERIFICATION
			);
		}
		if (requirement.aiOnlyDecision()) {
			return rejected(
					requirement,
					ExecutionReadinessRejectionReason.AI_ONLY_DECISION_NOT_ACCEPTABLE
			);
		}
		if (requirement.critical()
				&& (!requirement.approvalCompleted()
						|| !requirement.explicitAuthorizationPresent()
						|| !requirement.rollbackReviewCompleted()
						|| !requirement.verificationReviewCompleted()
						|| requirement.executionAuditDecision().integrity()
								!= ExecutionAuditIntegrity.VERIFIED)) {
			return rejected(
					requirement,
					ExecutionReadinessRejectionReason
							.CRITICAL_REQUIREMENTS_NOT_SATISFIED
			);
		}

		return new ExecutionReadinessDecision(
				true,
				ExecutionReadinessScope.READY,
				requirement,
				null
		);
	}

	private ExecutionReadinessDecision rejected(
			ExecutionReadinessRequirement requirement,
			ExecutionReadinessRejectionReason rejectionReason
	) {
		return new ExecutionReadinessDecision(
				false,
				ExecutionReadinessScope.NONE,
				requirement,
				rejectionReason
		);
	}
}
