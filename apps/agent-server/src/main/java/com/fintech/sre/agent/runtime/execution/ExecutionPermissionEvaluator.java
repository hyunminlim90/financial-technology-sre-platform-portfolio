package com.fintech.sre.agent.runtime.execution;

import java.util.Objects;

import com.fintech.sre.agent.runtime.action.ActionCommandIntegrationResult;
import com.fintech.sre.agent.runtime.action.ActionCommandIntegrationStatus;
import com.fintech.sre.agent.runtime.reliability.OperationalUncertainty;

public class ExecutionPermissionEvaluator {

	public ExecutionPermission evaluate(
			ActionCommandIntegrationResult actionCommandIntegration,
			String executionPermissionIdentifier,
			String executionPolicy,
			String operatorAuthorization,
			boolean executionGuardrailPresent,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		Objects.requireNonNull(
				actionCommandIntegration,
				"actionCommandIntegration must not be null"
		);
		Objects.requireNonNull(lifecycleRisk, "lifecycleRisk must not be null");

		return new ExecutionPermission(
				level(
						actionCommandIntegration,
						executionPermissionIdentifier,
						executionPolicy,
						operatorAuthorization,
						executionGuardrailPresent,
						lifecycleRisk,
						paymentSafetyUncertainty
				),
				reason(
						actionCommandIntegration,
						executionPermissionIdentifier,
						executionPolicy,
						operatorAuthorization,
						executionGuardrailPresent,
						lifecycleRisk,
						paymentSafetyUncertainty
				),
				scope(
						actionCommandIntegration,
						executionPermissionIdentifier,
						executionPolicy,
						operatorAuthorization,
						executionGuardrailPresent,
						lifecycleRisk,
						paymentSafetyUncertainty
				),
				actionCommandIntegration,
				executionPermissionIdentifier,
				executionPolicy,
				operatorAuthorization,
				executionGuardrailPresent,
				lifecycleRisk,
				paymentSafetyUncertainty
		);
	}

	private ExecutionPermissionLevel level(
			ActionCommandIntegrationResult actionCommandIntegration,
			String executionPermissionIdentifier,
			String executionPolicy,
			String operatorAuthorization,
			boolean executionGuardrailPresent,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		if (paymentSafetyUncertainty) {
			return ExecutionPermissionLevel.BLOCKED;
		}
		if (lifecycleRisk == OperationalUncertainty.CRITICAL) {
			return ExecutionPermissionLevel.BLOCKED;
		}
		if (missingText(executionPermissionIdentifier)) {
			return ExecutionPermissionLevel.BLOCKED;
		}
		if (missingText(executionPolicy)) {
			return ExecutionPermissionLevel.BLOCKED;
		}
		if (missingText(operatorAuthorization)) {
			return ExecutionPermissionLevel.BLOCKED;
		}
		if (!executionGuardrailPresent) {
			return ExecutionPermissionLevel.BLOCKED;
		}

		return switch (actionCommandIntegration.status()) {
			case ACTION_COMMAND_CANDIDATE_READY -> ExecutionPermissionLevel.EXECUTION_PERMITTED;
			case PARTIAL_ACTION_COMMAND -> ExecutionPermissionLevel.PARTIAL;
			case NOT_READY -> ExecutionPermissionLevel.NOT_READY;
			case UNRELIABLE -> ExecutionPermissionLevel.UNRELIABLE;
			case BLOCKED -> ExecutionPermissionLevel.BLOCKED;
			case UNKNOWN -> ExecutionPermissionLevel.UNKNOWN;
		};
	}

	private ExecutionPermissionReason reason(
			ActionCommandIntegrationResult actionCommandIntegration,
			String executionPermissionIdentifier,
			String executionPolicy,
			String operatorAuthorization,
			boolean executionGuardrailPresent,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		if (paymentSafetyUncertainty) {
			return ExecutionPermissionReason.PAYMENT_SAFETY_UNCERTAINTY;
		}
		if (lifecycleRisk == OperationalUncertainty.CRITICAL) {
			return ExecutionPermissionReason.CRITICAL_LIFECYCLE_RISK;
		}
		if (missingText(executionPermissionIdentifier)) {
			return ExecutionPermissionReason.MISSING_EXECUTION_PERMISSION_IDENTIFIER;
		}
		if (missingText(executionPolicy)) {
			return ExecutionPermissionReason.MISSING_EXECUTION_POLICY;
		}
		if (missingText(operatorAuthorization)) {
			return ExecutionPermissionReason.MISSING_OPERATOR_AUTHORIZATION;
		}
		if (!executionGuardrailPresent) {
			return ExecutionPermissionReason.MISSING_EXECUTION_GUARDRAIL;
		}

		return switch (actionCommandIntegration.status()) {
			case ACTION_COMMAND_CANDIDATE_READY -> ExecutionPermissionReason.ACTION_COMMAND_CANDIDATE_READY;
			case PARTIAL_ACTION_COMMAND -> ExecutionPermissionReason.PARTIAL_ACTION_COMMAND;
			case NOT_READY -> ExecutionPermissionReason.NOT_READY_ACTION_COMMAND;
			case UNRELIABLE -> ExecutionPermissionReason.UNRELIABLE_ACTION_COMMAND;
			case BLOCKED -> ExecutionPermissionReason.BLOCKED_ACTION_COMMAND;
			case UNKNOWN -> ExecutionPermissionReason.UNKNOWN;
		};
	}

	private ExecutionPermissionScope scope(
			ActionCommandIntegrationResult actionCommandIntegration,
			String executionPermissionIdentifier,
			String executionPolicy,
			String operatorAuthorization,
			boolean executionGuardrailPresent,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		if (paymentSafetyUncertainty) {
			return ExecutionPermissionScope.PAYMENT_SAFETY;
		}
		if (lifecycleRisk == OperationalUncertainty.CRITICAL) {
			return ExecutionPermissionScope.LIFECYCLE_RISK;
		}
		if (missingText(executionPermissionIdentifier)) {
			return ExecutionPermissionScope.EXECUTION_PERMISSION;
		}
		if (missingText(executionPolicy)) {
			return ExecutionPermissionScope.EXECUTION_POLICY;
		}
		if (missingText(operatorAuthorization)) {
			return ExecutionPermissionScope.OPERATOR_AUTHORIZATION;
		}
		if (!executionGuardrailPresent) {
			return ExecutionPermissionScope.EXECUTION_GUARDRAIL;
		}

		return ExecutionPermissionScope.EXECUTION_PERMISSION;
	}

	private boolean missingText(String value) {
		return value == null || value.isBlank();
	}
}
