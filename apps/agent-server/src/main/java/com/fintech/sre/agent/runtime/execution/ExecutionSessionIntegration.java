package com.fintech.sre.agent.runtime.execution;

import com.fintech.sre.agent.runtime.reliability.OperationalUncertainty;

public class ExecutionSessionIntegration {

	public ExecutionSessionIntegrationResult integrate(
			ExecutionSession executionSession
	) {
		if (executionSession == null) {
			throw new NullPointerException("executionSession must not be null");
		}

		if (executionSession.paymentSafetyUncertainty()) {
			return result(
					executionSession,
					ExecutionSessionIntegrationStatus.BLOCKED,
					ExecutionSessionIntegrationReason.PAYMENT_SAFETY_UNCERTAINTY,
					ExecutionSessionIntegrationScope.PAYMENT_SAFETY,
					false,
					false
			);
		}
		if (executionSession.lifecycleRisk() == OperationalUncertainty.CRITICAL) {
			return result(
					executionSession,
					ExecutionSessionIntegrationStatus.BLOCKED,
					ExecutionSessionIntegrationReason.CRITICAL_LIFECYCLE_RISK,
					ExecutionSessionIntegrationScope.LIFECYCLE_RISK,
					false,
					false
			);
		}
		if (missingSessionIdentifier(executionSession)) {
			return result(
					executionSession,
					ExecutionSessionIntegrationStatus.BLOCKED,
					ExecutionSessionIntegrationReason.MISSING_SESSION_IDENTIFIER,
					ExecutionSessionIntegrationScope.EXECUTION_SESSION,
					false,
					false
			);
		}
		if (missingExecutionCorrelationIdentifier(executionSession)) {
			return result(
					executionSession,
					ExecutionSessionIntegrationStatus.BLOCKED,
					ExecutionSessionIntegrationReason.MISSING_EXECUTION_CORRELATION_IDENTIFIER,
					ExecutionSessionIntegrationScope.EXECUTION_CORRELATION,
					false,
					false
			);
		}
		if (missingExecutionScope(executionSession)) {
			return result(
					executionSession,
					ExecutionSessionIntegrationStatus.BLOCKED,
					ExecutionSessionIntegrationReason.MISSING_EXECUTION_SCOPE,
					ExecutionSessionIntegrationScope.EXECUTION_SCOPE,
					false,
					false
			);
		}
		if (missingSessionPolicy(executionSession)) {
			return result(
					executionSession,
					ExecutionSessionIntegrationStatus.BLOCKED,
					ExecutionSessionIntegrationReason.MISSING_SESSION_POLICY,
					ExecutionSessionIntegrationScope.SESSION_POLICY,
					false,
					false
			);
		}

		return switch (executionSession.level()) {
			case EXECUTION_SESSION_READY -> result(
					executionSession,
					ExecutionSessionIntegrationStatus.EXECUTION_SESSION_READY_VIEW,
					ExecutionSessionIntegrationReason.EXECUTION_SESSION_READY,
					ExecutionSessionIntegrationScope.OPERATOR_VIEW,
					true,
					true
			);
			case PARTIAL -> result(
					executionSession,
					ExecutionSessionIntegrationStatus.PARTIAL_EXECUTION_SESSION,
					ExecutionSessionIntegrationReason.PARTIAL_EXECUTION_SESSION,
					ExecutionSessionIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case NOT_READY -> result(
					executionSession,
					ExecutionSessionIntegrationStatus.NOT_READY,
					ExecutionSessionIntegrationReason.NOT_READY_EXECUTION_SESSION,
					ExecutionSessionIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case UNRELIABLE -> result(
					executionSession,
					ExecutionSessionIntegrationStatus.UNRELIABLE,
					ExecutionSessionIntegrationReason.UNRELIABLE_EXECUTION_SESSION,
					ExecutionSessionIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case BLOCKED -> result(
					executionSession,
					ExecutionSessionIntegrationStatus.BLOCKED,
					ExecutionSessionIntegrationReason.BLOCKED_EXECUTION_SESSION,
					ExecutionSessionIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case UNKNOWN -> result(
					executionSession,
					ExecutionSessionIntegrationStatus.UNKNOWN,
					ExecutionSessionIntegrationReason.UNKNOWN,
					ExecutionSessionIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
		};
	}

	public boolean readOnly() {
		return true;
	}

	public boolean actualSessionCreation() {
		return false;
	}

	public boolean threadCreation() {
		return false;
	}

	public boolean transactionStart() {
		return false;
	}

	public boolean kubernetesJobCreation() {
		return false;
	}

	public boolean podCreation() {
		return false;
	}

	public boolean workflowExecution() {
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

	private boolean missingSessionIdentifier(ExecutionSession executionSession) {
		return executionSession.sessionIdentifier() == null
				|| executionSession.sessionIdentifier().isBlank();
	}

	private boolean missingExecutionCorrelationIdentifier(
			ExecutionSession executionSession
	) {
		return executionSession.executionCorrelationIdentifier() == null
				|| executionSession.executionCorrelationIdentifier().isBlank();
	}

	private boolean missingExecutionScope(ExecutionSession executionSession) {
		return executionSession.executionScope() == null
				|| executionSession.executionScope().isBlank();
	}

	private boolean missingSessionPolicy(ExecutionSession executionSession) {
		return executionSession.sessionPolicy() == null
				|| executionSession.sessionPolicy().isBlank();
	}

	private ExecutionSessionIntegrationResult result(
			ExecutionSession executionSession,
			ExecutionSessionIntegrationStatus status,
			ExecutionSessionIntegrationReason reason,
			ExecutionSessionIntegrationScope scope,
			boolean operatorFacingExecutionSessionVisible,
			boolean executionSessionCertaintyAllowed
	) {
		return new ExecutionSessionIntegrationResult(
				executionSession,
				status,
				reason,
				scope,
				operatorFacingExecutionSessionVisible,
				executionSessionCertaintyAllowed
		);
	}
}
