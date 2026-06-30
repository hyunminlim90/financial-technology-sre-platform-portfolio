package com.fintech.sre.agent.runtime.execution;

import com.fintech.sre.agent.runtime.reliability.OperationalUncertainty;

public class ExecutionContextIntegration {

	public ExecutionContextIntegrationResult integrate(
			ExecutionContext executionContext
	) {
		if (executionContext == null) {
			throw new NullPointerException("executionContext must not be null");
		}

		if (executionContext.paymentSafetyUncertainty()) {
			return result(
					executionContext,
					ExecutionContextIntegrationStatus.BLOCKED,
					ExecutionContextIntegrationReason.PAYMENT_SAFETY_UNCERTAINTY,
					ExecutionContextIntegrationScope.PAYMENT_SAFETY,
					false,
					false
			);
		}
		if (executionContext.lifecycleRisk() == OperationalUncertainty.CRITICAL) {
			return result(
					executionContext,
					ExecutionContextIntegrationStatus.BLOCKED,
					ExecutionContextIntegrationReason.CRITICAL_LIFECYCLE_RISK,
					ExecutionContextIntegrationScope.LIFECYCLE_RISK,
					false,
					false
			);
		}
		if (missingContextIdentifier(executionContext)) {
			return result(
					executionContext,
					ExecutionContextIntegrationStatus.BLOCKED,
					ExecutionContextIntegrationReason.MISSING_CONTEXT_IDENTIFIER,
					ExecutionContextIntegrationScope.EXECUTION_CONTEXT,
					false,
					false
			);
		}
		if (missingExecutionContextScope(executionContext)) {
			return result(
					executionContext,
					ExecutionContextIntegrationStatus.BLOCKED,
					ExecutionContextIntegrationReason.MISSING_EXECUTION_CONTEXT_SCOPE,
					ExecutionContextIntegrationScope.EXECUTION_CONTEXT_SCOPE,
					false,
					false
			);
		}
		if (missingExecutionMetadata(executionContext)) {
			return result(
					executionContext,
					ExecutionContextIntegrationStatus.BLOCKED,
					ExecutionContextIntegrationReason.MISSING_EXECUTION_METADATA,
					ExecutionContextIntegrationScope.EXECUTION_METADATA,
					false,
					false
			);
		}
		if (missingContextPolicy(executionContext)) {
			return result(
					executionContext,
					ExecutionContextIntegrationStatus.BLOCKED,
					ExecutionContextIntegrationReason.MISSING_CONTEXT_POLICY,
					ExecutionContextIntegrationScope.CONTEXT_POLICY,
					false,
					false
			);
		}

		return switch (executionContext.level()) {
			case EXECUTION_CONTEXT_READY -> result(
					executionContext,
					ExecutionContextIntegrationStatus.EXECUTION_CONTEXT_READY_VIEW,
					ExecutionContextIntegrationReason.EXECUTION_CONTEXT_READY,
					ExecutionContextIntegrationScope.OPERATOR_VIEW,
					true,
					true
			);
			case PARTIAL -> result(
					executionContext,
					ExecutionContextIntegrationStatus.PARTIAL_EXECUTION_CONTEXT,
					ExecutionContextIntegrationReason.PARTIAL_EXECUTION_CONTEXT,
					ExecutionContextIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case NOT_READY -> result(
					executionContext,
					ExecutionContextIntegrationStatus.NOT_READY,
					ExecutionContextIntegrationReason.NOT_READY_EXECUTION_CONTEXT,
					ExecutionContextIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case UNRELIABLE -> result(
					executionContext,
					ExecutionContextIntegrationStatus.UNRELIABLE,
					ExecutionContextIntegrationReason.UNRELIABLE_EXECUTION_CONTEXT,
					ExecutionContextIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case BLOCKED -> result(
					executionContext,
					ExecutionContextIntegrationStatus.BLOCKED,
					ExecutionContextIntegrationReason.BLOCKED_EXECUTION_CONTEXT,
					ExecutionContextIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case UNKNOWN -> result(
					executionContext,
					ExecutionContextIntegrationStatus.UNKNOWN,
					ExecutionContextIntegrationReason.UNKNOWN,
					ExecutionContextIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
		};
	}

	public boolean readOnly() {
		return true;
	}

	public boolean actualContextCreation() {
		return false;
	}

	public boolean threadLocalCreation() {
		return false;
	}

	public boolean securityContextCreation() {
		return false;
	}

	public boolean transactionContextCreation() {
		return false;
	}

	public boolean kubernetesContextCreation() {
		return false;
	}

	public boolean runtimeExecution() {
		return false;
	}

	public boolean actionExecution() {
		return false;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}

	private boolean missingContextIdentifier(ExecutionContext executionContext) {
		return executionContext.contextIdentifier() == null
				|| executionContext.contextIdentifier().isBlank();
	}

	private boolean missingExecutionContextScope(ExecutionContext executionContext) {
		return executionContext.executionContextScope() == null
				|| executionContext.executionContextScope().isBlank();
	}

	private boolean missingExecutionMetadata(ExecutionContext executionContext) {
		return executionContext.executionMetadata() == null
				|| executionContext.executionMetadata().isBlank();
	}

	private boolean missingContextPolicy(ExecutionContext executionContext) {
		return executionContext.contextPolicy() == null
				|| executionContext.contextPolicy().isBlank();
	}

	private ExecutionContextIntegrationResult result(
			ExecutionContext executionContext,
			ExecutionContextIntegrationStatus status,
			ExecutionContextIntegrationReason reason,
			ExecutionContextIntegrationScope scope,
			boolean operatorFacingExecutionContextVisible,
			boolean executionContextCertaintyAllowed
	) {
		return new ExecutionContextIntegrationResult(
				executionContext,
				status,
				reason,
				scope,
				operatorFacingExecutionContextVisible,
				executionContextCertaintyAllowed
		);
	}
}
