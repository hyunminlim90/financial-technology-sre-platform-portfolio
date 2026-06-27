package com.fintech.sre.agent.runtime.execution;

import com.fintech.sre.agent.runtime.reliability.OperationalUncertainty;

public class ExecutionDispatchIntegration {

	public ExecutionDispatchIntegrationResult integrate(
			ExecutionDispatch executionDispatch
	) {
		if (executionDispatch == null) {
			throw new NullPointerException("executionDispatch must not be null");
		}

		if (executionDispatch.paymentSafetyUncertainty()) {
			return result(
					executionDispatch,
					ExecutionDispatchIntegrationStatus.BLOCKED,
					ExecutionDispatchIntegrationReason.PAYMENT_SAFETY_UNCERTAINTY,
					ExecutionDispatchIntegrationScope.PAYMENT_SAFETY,
					false,
					false
			);
		}
		if (executionDispatch.lifecycleRisk() == OperationalUncertainty.CRITICAL) {
			return result(
					executionDispatch,
					ExecutionDispatchIntegrationStatus.BLOCKED,
					ExecutionDispatchIntegrationReason.CRITICAL_LIFECYCLE_RISK,
					ExecutionDispatchIntegrationScope.LIFECYCLE_RISK,
					false,
					false
			);
		}
		if (missingDispatchIdentifier(executionDispatch)) {
			return result(
					executionDispatch,
					ExecutionDispatchIntegrationStatus.BLOCKED,
					ExecutionDispatchIntegrationReason.MISSING_DISPATCH_IDENTIFIER,
					ExecutionDispatchIntegrationScope.EXECUTION_DISPATCH,
					false,
					false
			);
		}
		if (missingExecutionEndpoint(executionDispatch)) {
			return result(
					executionDispatch,
					ExecutionDispatchIntegrationStatus.BLOCKED,
					ExecutionDispatchIntegrationReason.MISSING_EXECUTION_ENDPOINT,
					ExecutionDispatchIntegrationScope.EXECUTION_ENDPOINT,
					false,
					false
			);
		}
		if (missingDispatchPolicy(executionDispatch)) {
			return result(
					executionDispatch,
					ExecutionDispatchIntegrationStatus.BLOCKED,
					ExecutionDispatchIntegrationReason.MISSING_DISPATCH_POLICY,
					ExecutionDispatchIntegrationScope.DISPATCH_POLICY,
					false,
					false
			);
		}
		if (!executionDispatch.dispatchGuardrailPresent()) {
			return result(
					executionDispatch,
					ExecutionDispatchIntegrationStatus.BLOCKED,
					ExecutionDispatchIntegrationReason.MISSING_DISPATCH_GUARDRAIL,
					ExecutionDispatchIntegrationScope.DISPATCH_GUARDRAIL,
					false,
					false
			);
		}

		return switch (executionDispatch.level()) {
			case DISPATCH_READY -> result(
					executionDispatch,
					ExecutionDispatchIntegrationStatus.DISPATCH_READY_VIEW,
					ExecutionDispatchIntegrationReason.DISPATCH_READY,
					ExecutionDispatchIntegrationScope.OPERATOR_VIEW,
					true,
					true
			);
			case PARTIAL -> result(
					executionDispatch,
					ExecutionDispatchIntegrationStatus.PARTIAL_DISPATCH,
					ExecutionDispatchIntegrationReason.PARTIAL_DISPATCH,
					ExecutionDispatchIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case NOT_READY -> result(
					executionDispatch,
					ExecutionDispatchIntegrationStatus.NOT_READY,
					ExecutionDispatchIntegrationReason.NOT_READY_DISPATCH,
					ExecutionDispatchIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case UNRELIABLE -> result(
					executionDispatch,
					ExecutionDispatchIntegrationStatus.UNRELIABLE,
					ExecutionDispatchIntegrationReason.UNRELIABLE_DISPATCH,
					ExecutionDispatchIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case BLOCKED -> result(
					executionDispatch,
					ExecutionDispatchIntegrationStatus.BLOCKED,
					ExecutionDispatchIntegrationReason.BLOCKED_DISPATCH,
					ExecutionDispatchIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case UNKNOWN -> result(
					executionDispatch,
					ExecutionDispatchIntegrationStatus.UNKNOWN,
					ExecutionDispatchIntegrationReason.UNKNOWN,
					ExecutionDispatchIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
		};
	}

	public boolean readOnly() {
		return true;
	}

	public boolean dispatchPerformed() {
		return false;
	}

	public boolean actionExecution() {
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

	public boolean executionEngineCall() {
		return false;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}

	private boolean missingDispatchIdentifier(ExecutionDispatch executionDispatch) {
		return executionDispatch.dispatchIdentifier() == null
				|| executionDispatch.dispatchIdentifier().isBlank();
	}

	private boolean missingExecutionEndpoint(ExecutionDispatch executionDispatch) {
		return executionDispatch.executionEndpoint() == null
				|| executionDispatch.executionEndpoint().isBlank();
	}

	private boolean missingDispatchPolicy(ExecutionDispatch executionDispatch) {
		return executionDispatch.dispatchPolicy() == null
				|| executionDispatch.dispatchPolicy().isBlank();
	}

	private ExecutionDispatchIntegrationResult result(
			ExecutionDispatch executionDispatch,
			ExecutionDispatchIntegrationStatus status,
			ExecutionDispatchIntegrationReason reason,
			ExecutionDispatchIntegrationScope scope,
			boolean operatorFacingDispatchVisible,
			boolean dispatchCertaintyAllowed
	) {
		return new ExecutionDispatchIntegrationResult(
				executionDispatch,
				status,
				reason,
				scope,
				operatorFacingDispatchVisible,
				dispatchCertaintyAllowed
		);
	}
}
