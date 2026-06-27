package com.fintech.sre.agent.runtime.execution;

import com.fintech.sre.agent.runtime.reliability.OperationalUncertainty;

public class ExecutionPermissionIntegration {

	public ExecutionPermissionIntegrationResult integrate(
			ExecutionPermission executionPermission
	) {
		if (executionPermission == null) {
			throw new NullPointerException("executionPermission must not be null");
		}

		if (executionPermission.paymentSafetyUncertainty()) {
			return result(
					executionPermission,
					ExecutionPermissionIntegrationStatus.BLOCKED,
					ExecutionPermissionIntegrationReason.PAYMENT_SAFETY_UNCERTAINTY,
					ExecutionPermissionIntegrationScope.PAYMENT_SAFETY,
					false,
					false
			);
		}
		if (executionPermission.lifecycleRisk() == OperationalUncertainty.CRITICAL) {
			return result(
					executionPermission,
					ExecutionPermissionIntegrationStatus.BLOCKED,
					ExecutionPermissionIntegrationReason.CRITICAL_LIFECYCLE_RISK,
					ExecutionPermissionIntegrationScope.LIFECYCLE_RISK,
					false,
					false
			);
		}
		if (missingExecutionPermissionIdentifier(executionPermission)) {
			return result(
					executionPermission,
					ExecutionPermissionIntegrationStatus.BLOCKED,
					ExecutionPermissionIntegrationReason.MISSING_EXECUTION_PERMISSION_IDENTIFIER,
					ExecutionPermissionIntegrationScope.EXECUTION_PERMISSION,
					false,
					false
			);
		}
		if (missingExecutionPolicy(executionPermission)) {
			return result(
					executionPermission,
					ExecutionPermissionIntegrationStatus.BLOCKED,
					ExecutionPermissionIntegrationReason.MISSING_EXECUTION_POLICY,
					ExecutionPermissionIntegrationScope.EXECUTION_POLICY,
					false,
					false
			);
		}
		if (missingOperatorAuthorization(executionPermission)) {
			return result(
					executionPermission,
					ExecutionPermissionIntegrationStatus.BLOCKED,
					ExecutionPermissionIntegrationReason.MISSING_OPERATOR_AUTHORIZATION,
					ExecutionPermissionIntegrationScope.OPERATOR_AUTHORIZATION,
					false,
					false
			);
		}
		if (!executionPermission.executionGuardrailPresent()) {
			return result(
					executionPermission,
					ExecutionPermissionIntegrationStatus.BLOCKED,
					ExecutionPermissionIntegrationReason.MISSING_EXECUTION_GUARDRAIL,
					ExecutionPermissionIntegrationScope.EXECUTION_GUARDRAIL,
					false,
					false
			);
		}

		return switch (executionPermission.level()) {
			case EXECUTION_PERMITTED -> result(
					executionPermission,
					ExecutionPermissionIntegrationStatus.EXECUTION_PERMISSION_READY,
					ExecutionPermissionIntegrationReason.EXECUTION_PERMITTED,
					ExecutionPermissionIntegrationScope.OPERATOR_VIEW,
					true,
					true
			);
			case PARTIAL -> result(
					executionPermission,
					ExecutionPermissionIntegrationStatus.PARTIAL_EXECUTION_PERMISSION,
					ExecutionPermissionIntegrationReason.PARTIAL_EXECUTION_PERMISSION,
					ExecutionPermissionIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case NOT_READY -> result(
					executionPermission,
					ExecutionPermissionIntegrationStatus.NOT_READY,
					ExecutionPermissionIntegrationReason.NOT_READY_EXECUTION_PERMISSION,
					ExecutionPermissionIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case UNRELIABLE -> result(
					executionPermission,
					ExecutionPermissionIntegrationStatus.UNRELIABLE,
					ExecutionPermissionIntegrationReason.UNRELIABLE_EXECUTION_PERMISSION,
					ExecutionPermissionIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case BLOCKED -> result(
					executionPermission,
					ExecutionPermissionIntegrationStatus.BLOCKED,
					ExecutionPermissionIntegrationReason.BLOCKED_EXECUTION_PERMISSION,
					ExecutionPermissionIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case UNKNOWN -> result(
					executionPermission,
					ExecutionPermissionIntegrationStatus.UNKNOWN,
					ExecutionPermissionIntegrationReason.UNKNOWN,
					ExecutionPermissionIntegrationScope.OPERATOR_VIEW,
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

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}

	private boolean missingExecutionPermissionIdentifier(
			ExecutionPermission executionPermission
	) {
		return executionPermission.executionPermissionIdentifier() == null
				|| executionPermission.executionPermissionIdentifier().isBlank();
	}

	private boolean missingExecutionPolicy(ExecutionPermission executionPermission) {
		return executionPermission.executionPolicy() == null
				|| executionPermission.executionPolicy().isBlank();
	}

	private boolean missingOperatorAuthorization(
			ExecutionPermission executionPermission
	) {
		return executionPermission.operatorAuthorization() == null
				|| executionPermission.operatorAuthorization().isBlank();
	}

	private ExecutionPermissionIntegrationResult result(
			ExecutionPermission executionPermission,
			ExecutionPermissionIntegrationStatus status,
			ExecutionPermissionIntegrationReason reason,
			ExecutionPermissionIntegrationScope scope,
			boolean operatorFacingExecutionPermissionVisible,
			boolean executionPermissionCertaintyAllowed
	) {
		return new ExecutionPermissionIntegrationResult(
				executionPermission,
				status,
				reason,
				scope,
				operatorFacingExecutionPermissionVisible,
				executionPermissionCertaintyAllowed
		);
	}
}
