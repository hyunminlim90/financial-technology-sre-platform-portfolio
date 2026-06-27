package com.fintech.sre.agent.runtime.execution;

import com.fintech.sre.agent.runtime.reliability.OperationalUncertainty;

public class ExecutionPlanIntegration {

	public ExecutionPlanIntegrationResult integrate(ExecutionPlan executionPlan) {
		if (executionPlan == null) {
			throw new NullPointerException("executionPlan must not be null");
		}

		if (executionPlan.paymentSafetyUncertainty()) {
			return result(
					executionPlan,
					ExecutionPlanIntegrationStatus.BLOCKED,
					ExecutionPlanIntegrationReason.PAYMENT_SAFETY_UNCERTAINTY,
					ExecutionPlanIntegrationScope.PAYMENT_SAFETY,
					false,
					false
			);
		}
		if (executionPlan.lifecycleRisk() == OperationalUncertainty.CRITICAL) {
			return result(
					executionPlan,
					ExecutionPlanIntegrationStatus.BLOCKED,
					ExecutionPlanIntegrationReason.CRITICAL_LIFECYCLE_RISK,
					ExecutionPlanIntegrationScope.LIFECYCLE_RISK,
					false,
					false
			);
		}
		if (missingExecutionPlanIdentifier(executionPlan)) {
			return result(
					executionPlan,
					ExecutionPlanIntegrationStatus.BLOCKED,
					ExecutionPlanIntegrationReason.MISSING_EXECUTION_PLAN_IDENTIFIER,
					ExecutionPlanIntegrationScope.EXECUTION_PLAN,
					false,
					false
			);
		}
		if (missingExecutionSequence(executionPlan)) {
			return result(
					executionPlan,
					ExecutionPlanIntegrationStatus.BLOCKED,
					ExecutionPlanIntegrationReason.MISSING_EXECUTION_SEQUENCE,
					ExecutionPlanIntegrationScope.EXECUTION_SEQUENCE,
					false,
					false
			);
		}
		if (!executionPlan.rollbackPlanPresent()) {
			return result(
					executionPlan,
					ExecutionPlanIntegrationStatus.BLOCKED,
					ExecutionPlanIntegrationReason.MISSING_ROLLBACK_PLAN,
					ExecutionPlanIntegrationScope.ROLLBACK_PLAN,
					false,
					false
			);
		}
		if (!executionPlan.verificationPlanPresent()) {
			return result(
					executionPlan,
					ExecutionPlanIntegrationStatus.BLOCKED,
					ExecutionPlanIntegrationReason.MISSING_VERIFICATION_PLAN,
					ExecutionPlanIntegrationScope.VERIFICATION_PLAN,
					false,
					false
			);
		}

		return switch (executionPlan.level()) {
			case EXECUTION_PLAN_READY -> result(
					executionPlan,
					ExecutionPlanIntegrationStatus.EXECUTION_PLAN_READY_VIEW,
					ExecutionPlanIntegrationReason.EXECUTION_PLAN_READY,
					ExecutionPlanIntegrationScope.OPERATOR_VIEW,
					true,
					true
			);
			case PARTIAL -> result(
					executionPlan,
					ExecutionPlanIntegrationStatus.PARTIAL_EXECUTION_PLAN,
					ExecutionPlanIntegrationReason.PARTIAL_EXECUTION_PLAN,
					ExecutionPlanIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case NOT_READY -> result(
					executionPlan,
					ExecutionPlanIntegrationStatus.NOT_READY,
					ExecutionPlanIntegrationReason.NOT_READY_EXECUTION_PLAN,
					ExecutionPlanIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case UNRELIABLE -> result(
					executionPlan,
					ExecutionPlanIntegrationStatus.UNRELIABLE,
					ExecutionPlanIntegrationReason.UNRELIABLE_EXECUTION_PLAN,
					ExecutionPlanIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case BLOCKED -> result(
					executionPlan,
					ExecutionPlanIntegrationStatus.BLOCKED,
					ExecutionPlanIntegrationReason.BLOCKED_EXECUTION_PLAN,
					ExecutionPlanIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case UNKNOWN -> result(
					executionPlan,
					ExecutionPlanIntegrationStatus.UNKNOWN,
					ExecutionPlanIntegrationReason.UNKNOWN,
					ExecutionPlanIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
		};
	}

	public boolean readOnly() {
		return true;
	}

	public boolean actionExecution() {
		return false;
	}

	public boolean actionDispatch() {
		return false;
	}

	public boolean kubernetesApiCall() {
		return false;
	}

	public boolean kubectlExecution() {
		return false;
	}

	public boolean argoCdSync() {
		return false;
	}

	public boolean terraformApply() {
		return false;
	}

	public boolean sshOrAnsibleExecution() {
		return false;
	}

	public boolean executionEngine() {
		return false;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}

	private boolean missingExecutionPlanIdentifier(ExecutionPlan executionPlan) {
		return executionPlan.executionPlanIdentifier() == null
				|| executionPlan.executionPlanIdentifier().isBlank();
	}

	private boolean missingExecutionSequence(ExecutionPlan executionPlan) {
		return executionPlan.executionSequence() == null
				|| executionPlan.executionSequence().isBlank();
	}

	private ExecutionPlanIntegrationResult result(
			ExecutionPlan executionPlan,
			ExecutionPlanIntegrationStatus status,
			ExecutionPlanIntegrationReason reason,
			ExecutionPlanIntegrationScope scope,
			boolean operatorFacingExecutionPlanVisible,
			boolean executionPlanCertaintyAllowed
	) {
		return new ExecutionPlanIntegrationResult(
				executionPlan,
				status,
				reason,
				scope,
				operatorFacingExecutionPlanVisible,
				executionPlanCertaintyAllowed
		);
	}
}
