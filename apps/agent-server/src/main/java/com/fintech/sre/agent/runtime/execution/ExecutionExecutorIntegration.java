package com.fintech.sre.agent.runtime.execution;

import com.fintech.sre.agent.runtime.reliability.OperationalUncertainty;

public class ExecutionExecutorIntegration {

	public ExecutionExecutorIntegrationResult integrate(
			ExecutionExecutor executionExecutor
	) {
		if (executionExecutor == null) {
			throw new NullPointerException("executionExecutor must not be null");
		}

		if (executionExecutor.paymentSafetyUncertainty()) {
			return result(
					executionExecutor,
					ExecutionExecutorIntegrationStatus.BLOCKED,
					ExecutionExecutorIntegrationReason.PAYMENT_SAFETY_UNCERTAINTY,
					ExecutionExecutorIntegrationScope.PAYMENT_SAFETY,
					false,
					false
			);
		}
		if (executionExecutor.lifecycleRisk() == OperationalUncertainty.CRITICAL) {
			return result(
					executionExecutor,
					ExecutionExecutorIntegrationStatus.BLOCKED,
					ExecutionExecutorIntegrationReason.CRITICAL_LIFECYCLE_RISK,
					ExecutionExecutorIntegrationScope.LIFECYCLE_RISK,
					false,
					false
			);
		}
		if (missingExecutorIdentifier(executionExecutor)) {
			return result(
					executionExecutor,
					ExecutionExecutorIntegrationStatus.BLOCKED,
					ExecutionExecutorIntegrationReason.MISSING_EXECUTOR_IDENTIFIER,
					ExecutionExecutorIntegrationScope.EXECUTION_EXECUTOR,
					false,
					false
			);
		}
		if (missingExecutionStrategy(executionExecutor)) {
			return result(
					executionExecutor,
					ExecutionExecutorIntegrationStatus.BLOCKED,
					ExecutionExecutorIntegrationReason.MISSING_EXECUTION_STRATEGY,
					ExecutionExecutorIntegrationScope.EXECUTION_STRATEGY,
					false,
					false
			);
		}
		if (missingExecutionBoundary(executionExecutor)) {
			return result(
					executionExecutor,
					ExecutionExecutorIntegrationStatus.BLOCKED,
					ExecutionExecutorIntegrationReason.MISSING_EXECUTION_BOUNDARY,
					ExecutionExecutorIntegrationScope.EXECUTION_BOUNDARY,
					false,
					false
			);
		}
		if (missingExecutorPolicy(executionExecutor)) {
			return result(
					executionExecutor,
					ExecutionExecutorIntegrationStatus.BLOCKED,
					ExecutionExecutorIntegrationReason.MISSING_EXECUTOR_POLICY,
					ExecutionExecutorIntegrationScope.EXECUTOR_POLICY,
					false,
					false
			);
		}

		return switch (executionExecutor.level()) {
			case EXECUTION_EXECUTOR_READY -> result(
					executionExecutor,
					ExecutionExecutorIntegrationStatus.EXECUTION_EXECUTOR_READY_VIEW,
					ExecutionExecutorIntegrationReason.EXECUTION_EXECUTOR_READY,
					ExecutionExecutorIntegrationScope.OPERATOR_VIEW,
					true,
					true
			);
			case PARTIAL -> result(
					executionExecutor,
					ExecutionExecutorIntegrationStatus.PARTIAL_EXECUTION_EXECUTOR,
					ExecutionExecutorIntegrationReason.PARTIAL_EXECUTION_EXECUTOR,
					ExecutionExecutorIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case NOT_READY -> result(
					executionExecutor,
					ExecutionExecutorIntegrationStatus.NOT_READY,
					ExecutionExecutorIntegrationReason.NOT_READY_EXECUTION_EXECUTOR,
					ExecutionExecutorIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case UNRELIABLE -> result(
					executionExecutor,
					ExecutionExecutorIntegrationStatus.UNRELIABLE,
					ExecutionExecutorIntegrationReason.UNRELIABLE_EXECUTION_EXECUTOR,
					ExecutionExecutorIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case BLOCKED -> result(
					executionExecutor,
					ExecutionExecutorIntegrationStatus.BLOCKED,
					ExecutionExecutorIntegrationReason.BLOCKED_EXECUTION_EXECUTOR,
					ExecutionExecutorIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case UNKNOWN -> result(
					executionExecutor,
					ExecutionExecutorIntegrationStatus.UNKNOWN,
					ExecutionExecutorIntegrationReason.UNKNOWN,
					ExecutionExecutorIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
		};
	}

	public boolean readOnly() {
		return true;
	}

	public boolean executorImplementation() {
		return false;
	}

	public boolean executorThreadCreation() {
		return false;
	}

	public boolean adapterInvocation() {
		return false;
	}

	public boolean actionExecution() {
		return false;
	}

	public boolean kubernetesOrArgoOrTerraformOrSshOrAnsibleCall() {
		return false;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}

	private boolean missingExecutorIdentifier(ExecutionExecutor executionExecutor) {
		return executionExecutor.executorIdentifier() == null
				|| executionExecutor.executorIdentifier().isBlank();
	}

	private boolean missingExecutionStrategy(ExecutionExecutor executionExecutor) {
		return executionExecutor.executionStrategy() == null
				|| executionExecutor.executionStrategy().isBlank();
	}

	private boolean missingExecutionBoundary(ExecutionExecutor executionExecutor) {
		return executionExecutor.executionBoundary() == null
				|| executionExecutor.executionBoundary().isBlank();
	}

	private boolean missingExecutorPolicy(ExecutionExecutor executionExecutor) {
		return executionExecutor.executorPolicy() == null
				|| executionExecutor.executorPolicy().isBlank();
	}

	private ExecutionExecutorIntegrationResult result(
			ExecutionExecutor executionExecutor,
			ExecutionExecutorIntegrationStatus status,
			ExecutionExecutorIntegrationReason reason,
			ExecutionExecutorIntegrationScope scope,
			boolean operatorFacingExecutionExecutorVisible,
			boolean executionExecutorCertaintyAllowed
	) {
		return new ExecutionExecutorIntegrationResult(
				executionExecutor,
				status,
				reason,
				scope,
				operatorFacingExecutionExecutorVisible,
				executionExecutorCertaintyAllowed
		);
	}
}
