package com.fintech.sre.agent.runtime.execution;

import com.fintech.sre.agent.runtime.reliability.OperationalUncertainty;

public class ExecutionAdapterIntegration {

	public ExecutionAdapterIntegrationResult integrate(
			ExecutionAdapter executionAdapter
	) {
		if (executionAdapter == null) {
			throw new NullPointerException("executionAdapter must not be null");
		}

		if (executionAdapter.paymentSafetyUncertainty()) {
			return result(
					executionAdapter,
					ExecutionAdapterIntegrationStatus.BLOCKED,
					ExecutionAdapterIntegrationReason.PAYMENT_SAFETY_UNCERTAINTY,
					ExecutionAdapterIntegrationScope.PAYMENT_SAFETY,
					false,
					false
			);
		}
		if (executionAdapter.lifecycleRisk() == OperationalUncertainty.CRITICAL) {
			return result(
					executionAdapter,
					ExecutionAdapterIntegrationStatus.BLOCKED,
					ExecutionAdapterIntegrationReason.CRITICAL_LIFECYCLE_RISK,
					ExecutionAdapterIntegrationScope.LIFECYCLE_RISK,
					false,
					false
			);
		}
		if (missingAdapterIdentifier(executionAdapter)) {
			return result(
					executionAdapter,
					ExecutionAdapterIntegrationStatus.BLOCKED,
					ExecutionAdapterIntegrationReason.MISSING_ADAPTER_IDENTIFIER,
					ExecutionAdapterIntegrationScope.EXECUTION_ADAPTER,
					false,
					false
			);
		}
		if (missingAdapterType(executionAdapter)) {
			return result(
					executionAdapter,
					ExecutionAdapterIntegrationStatus.BLOCKED,
					ExecutionAdapterIntegrationReason.MISSING_ADAPTER_TYPE,
					ExecutionAdapterIntegrationScope.ADAPTER_TYPE,
					false,
					false
			);
		}
		if (missingAdapterBinding(executionAdapter)) {
			return result(
					executionAdapter,
					ExecutionAdapterIntegrationStatus.BLOCKED,
					ExecutionAdapterIntegrationReason.MISSING_ADAPTER_BINDING,
					ExecutionAdapterIntegrationScope.ADAPTER_BINDING,
					false,
					false
			);
		}
		if (missingAdapterPolicy(executionAdapter)) {
			return result(
					executionAdapter,
					ExecutionAdapterIntegrationStatus.BLOCKED,
					ExecutionAdapterIntegrationReason.MISSING_ADAPTER_POLICY,
					ExecutionAdapterIntegrationScope.ADAPTER_POLICY,
					false,
					false
			);
		}

		return switch (executionAdapter.level()) {
			case EXECUTION_ADAPTER_READY -> result(
					executionAdapter,
					ExecutionAdapterIntegrationStatus.EXECUTION_ADAPTER_READY_VIEW,
					ExecutionAdapterIntegrationReason.EXECUTION_ADAPTER_READY,
					ExecutionAdapterIntegrationScope.OPERATOR_VIEW,
					true,
					true
			);
			case PARTIAL -> result(
					executionAdapter,
					ExecutionAdapterIntegrationStatus.PARTIAL_EXECUTION_ADAPTER,
					ExecutionAdapterIntegrationReason.PARTIAL_EXECUTION_ADAPTER,
					ExecutionAdapterIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case NOT_READY -> result(
					executionAdapter,
					ExecutionAdapterIntegrationStatus.NOT_READY,
					ExecutionAdapterIntegrationReason.NOT_READY_EXECUTION_ADAPTER,
					ExecutionAdapterIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case UNRELIABLE -> result(
					executionAdapter,
					ExecutionAdapterIntegrationStatus.UNRELIABLE,
					ExecutionAdapterIntegrationReason.UNRELIABLE_EXECUTION_ADAPTER,
					ExecutionAdapterIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case BLOCKED -> result(
					executionAdapter,
					ExecutionAdapterIntegrationStatus.BLOCKED,
					ExecutionAdapterIntegrationReason.BLOCKED_EXECUTION_ADAPTER,
					ExecutionAdapterIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case UNKNOWN -> result(
					executionAdapter,
					ExecutionAdapterIntegrationStatus.UNKNOWN,
					ExecutionAdapterIntegrationReason.UNKNOWN,
					ExecutionAdapterIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
		};
	}

	public boolean readOnly() {
		return true;
	}

	public boolean actualAdapterImplementation() {
		return false;
	}

	public boolean adapterInvocation() {
		return false;
	}

	public boolean kubernetesAdapter() {
		return false;
	}

	public boolean argoCdAdapter() {
		return false;
	}

	public boolean terraformOrOpenTofuAdapter() {
		return false;
	}

	public boolean sshOrAnsibleAdapter() {
		return false;
	}

	public boolean actionExecution() {
		return false;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}

	private boolean missingAdapterIdentifier(ExecutionAdapter executionAdapter) {
		return executionAdapter.adapterIdentifier() == null
				|| executionAdapter.adapterIdentifier().isBlank();
	}

	private boolean missingAdapterType(ExecutionAdapter executionAdapter) {
		return executionAdapter.adapterType() == null
				|| executionAdapter.adapterType().isBlank();
	}

	private boolean missingAdapterBinding(ExecutionAdapter executionAdapter) {
		return executionAdapter.adapterBinding() == null
				|| executionAdapter.adapterBinding().isBlank();
	}

	private boolean missingAdapterPolicy(ExecutionAdapter executionAdapter) {
		return executionAdapter.adapterPolicy() == null
				|| executionAdapter.adapterPolicy().isBlank();
	}

	private ExecutionAdapterIntegrationResult result(
			ExecutionAdapter executionAdapter,
			ExecutionAdapterIntegrationStatus status,
			ExecutionAdapterIntegrationReason reason,
			ExecutionAdapterIntegrationScope scope,
			boolean operatorFacingExecutionAdapterVisible,
			boolean executionAdapterCertaintyAllowed
	) {
		return new ExecutionAdapterIntegrationResult(
				executionAdapter,
				status,
				reason,
				scope,
				operatorFacingExecutionAdapterVisible,
				executionAdapterCertaintyAllowed
		);
	}
}
