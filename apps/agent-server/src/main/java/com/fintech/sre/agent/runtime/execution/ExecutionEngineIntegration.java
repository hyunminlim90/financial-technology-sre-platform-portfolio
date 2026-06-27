package com.fintech.sre.agent.runtime.execution;

import com.fintech.sre.agent.runtime.reliability.OperationalUncertainty;

public class ExecutionEngineIntegration {

	public ExecutionEngineIntegrationResult integrate(ExecutionEngine executionEngine) {
		if (executionEngine == null) {
			throw new NullPointerException("executionEngine must not be null");
		}

		if (executionEngine.paymentSafetyUncertainty()) {
			return result(
					executionEngine,
					ExecutionEngineIntegrationStatus.BLOCKED,
					ExecutionEngineIntegrationReason.PAYMENT_SAFETY_UNCERTAINTY,
					ExecutionEngineIntegrationScope.PAYMENT_SAFETY,
					false,
					false
			);
		}
		if (executionEngine.lifecycleRisk() == OperationalUncertainty.CRITICAL) {
			return result(
					executionEngine,
					ExecutionEngineIntegrationStatus.BLOCKED,
					ExecutionEngineIntegrationReason.CRITICAL_LIFECYCLE_RISK,
					ExecutionEngineIntegrationScope.LIFECYCLE_RISK,
					false,
					false
			);
		}
		if (missingExecutionEngineIdentifier(executionEngine)) {
			return result(
					executionEngine,
					ExecutionEngineIntegrationStatus.BLOCKED,
					ExecutionEngineIntegrationReason.MISSING_EXECUTION_ENGINE_IDENTIFIER,
					ExecutionEngineIntegrationScope.EXECUTION_ENGINE,
					false,
					false
			);
		}
		if (missingExecutionEngineType(executionEngine)) {
			return result(
					executionEngine,
					ExecutionEngineIntegrationStatus.BLOCKED,
					ExecutionEngineIntegrationReason.MISSING_EXECUTION_ENGINE_TYPE,
					ExecutionEngineIntegrationScope.EXECUTION_ENGINE_TYPE,
					false,
					false
			);
		}
		if (missingExecutionEndpointBinding(executionEngine)) {
			return result(
					executionEngine,
					ExecutionEngineIntegrationStatus.BLOCKED,
					ExecutionEngineIntegrationReason.MISSING_EXECUTION_ENDPOINT_BINDING,
					ExecutionEngineIntegrationScope.EXECUTION_ENDPOINT,
					false,
					false
			);
		}
		if (missingExecutionPolicy(executionEngine)) {
			return result(
					executionEngine,
					ExecutionEngineIntegrationStatus.BLOCKED,
					ExecutionEngineIntegrationReason.MISSING_EXECUTION_POLICY,
					ExecutionEngineIntegrationScope.EXECUTION_POLICY,
					false,
					false
			);
		}

		return switch (executionEngine.level()) {
			case EXECUTION_ENGINE_READY -> result(
					executionEngine,
					ExecutionEngineIntegrationStatus.EXECUTION_ENGINE_READY_VIEW,
					ExecutionEngineIntegrationReason.EXECUTION_ENGINE_READY,
					ExecutionEngineIntegrationScope.OPERATOR_VIEW,
					true,
					true
			);
			case PARTIAL -> result(
					executionEngine,
					ExecutionEngineIntegrationStatus.PARTIAL_EXECUTION_ENGINE,
					ExecutionEngineIntegrationReason.PARTIAL_EXECUTION_ENGINE,
					ExecutionEngineIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case NOT_READY -> result(
					executionEngine,
					ExecutionEngineIntegrationStatus.NOT_READY,
					ExecutionEngineIntegrationReason.NOT_READY_EXECUTION_ENGINE,
					ExecutionEngineIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case UNRELIABLE -> result(
					executionEngine,
					ExecutionEngineIntegrationStatus.UNRELIABLE,
					ExecutionEngineIntegrationReason.UNRELIABLE_EXECUTION_ENGINE,
					ExecutionEngineIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case BLOCKED -> result(
					executionEngine,
					ExecutionEngineIntegrationStatus.BLOCKED,
					ExecutionEngineIntegrationReason.BLOCKED_EXECUTION_ENGINE,
					ExecutionEngineIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case UNKNOWN -> result(
					executionEngine,
					ExecutionEngineIntegrationStatus.UNKNOWN,
					ExecutionEngineIntegrationReason.UNKNOWN,
					ExecutionEngineIntegrationScope.OPERATOR_VIEW,
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

	public boolean dispatchPerformed() {
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

	public boolean specificExecutionEngineImplementation() {
		return false;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}

	private boolean missingExecutionEngineIdentifier(ExecutionEngine executionEngine) {
		return executionEngine.executionEngineIdentifier() == null
				|| executionEngine.executionEngineIdentifier().isBlank();
	}

	private boolean missingExecutionEngineType(ExecutionEngine executionEngine) {
		return executionEngine.executionEngineType() == null
				|| executionEngine.executionEngineType().isBlank();
	}

	private boolean missingExecutionEndpointBinding(ExecutionEngine executionEngine) {
		return executionEngine.executionEndpointBinding() == null
				|| executionEngine.executionEndpointBinding().isBlank();
	}

	private boolean missingExecutionPolicy(ExecutionEngine executionEngine) {
		return executionEngine.executionPolicy() == null
				|| executionEngine.executionPolicy().isBlank();
	}

	private ExecutionEngineIntegrationResult result(
			ExecutionEngine executionEngine,
			ExecutionEngineIntegrationStatus status,
			ExecutionEngineIntegrationReason reason,
			ExecutionEngineIntegrationScope scope,
			boolean operatorFacingExecutionEngineVisible,
			boolean executionEngineCertaintyAllowed
	) {
		return new ExecutionEngineIntegrationResult(
				executionEngine,
				status,
				reason,
				scope,
				operatorFacingExecutionEngineVisible,
				executionEngineCertaintyAllowed
		);
	}
}
