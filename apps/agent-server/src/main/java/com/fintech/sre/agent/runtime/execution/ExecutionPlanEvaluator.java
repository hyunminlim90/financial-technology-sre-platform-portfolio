package com.fintech.sre.agent.runtime.execution;

import java.util.Objects;

import com.fintech.sre.agent.runtime.execution.ExecutionPermissionIntegrationStatus;
import com.fintech.sre.agent.runtime.reliability.OperationalUncertainty;

public class ExecutionPlanEvaluator {

	public ExecutionPlan evaluate(
			ExecutionPermissionIntegrationResult executionPermissionIntegration,
			String executionPlanIdentifier,
			String executionSequence,
			boolean rollbackPlanPresent,
			boolean verificationPlanPresent,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		Objects.requireNonNull(
				executionPermissionIntegration,
				"executionPermissionIntegration must not be null"
		);
		Objects.requireNonNull(lifecycleRisk, "lifecycleRisk must not be null");

		return new ExecutionPlan(
				level(
						executionPermissionIntegration,
						executionPlanIdentifier,
						executionSequence,
						rollbackPlanPresent,
						verificationPlanPresent,
						lifecycleRisk,
						paymentSafetyUncertainty
				),
				reason(
						executionPermissionIntegration,
						executionPlanIdentifier,
						executionSequence,
						rollbackPlanPresent,
						verificationPlanPresent,
						lifecycleRisk,
						paymentSafetyUncertainty
				),
				scope(
						executionPermissionIntegration,
						executionPlanIdentifier,
						executionSequence,
						rollbackPlanPresent,
						verificationPlanPresent,
						lifecycleRisk,
						paymentSafetyUncertainty
				),
				executionPermissionIntegration,
				executionPlanIdentifier,
				executionSequence,
				rollbackPlanPresent,
				verificationPlanPresent,
				lifecycleRisk,
				paymentSafetyUncertainty
		);
	}

	private ExecutionPlanLevel level(
			ExecutionPermissionIntegrationResult executionPermissionIntegration,
			String executionPlanIdentifier,
			String executionSequence,
			boolean rollbackPlanPresent,
			boolean verificationPlanPresent,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		if (paymentSafetyUncertainty) {
			return ExecutionPlanLevel.BLOCKED;
		}
		if (lifecycleRisk == OperationalUncertainty.CRITICAL) {
			return ExecutionPlanLevel.BLOCKED;
		}
		if (missingText(executionPlanIdentifier)) {
			return ExecutionPlanLevel.BLOCKED;
		}
		if (missingText(executionSequence)) {
			return ExecutionPlanLevel.BLOCKED;
		}
		if (!rollbackPlanPresent) {
			return ExecutionPlanLevel.BLOCKED;
		}
		if (!verificationPlanPresent) {
			return ExecutionPlanLevel.BLOCKED;
		}

		return switch (executionPermissionIntegration.status()) {
			case EXECUTION_PERMISSION_READY -> ExecutionPlanLevel.EXECUTION_PLAN_READY;
			case PARTIAL_EXECUTION_PERMISSION -> ExecutionPlanLevel.PARTIAL;
			case NOT_READY -> ExecutionPlanLevel.NOT_READY;
			case UNRELIABLE -> ExecutionPlanLevel.UNRELIABLE;
			case BLOCKED -> ExecutionPlanLevel.BLOCKED;
			case UNKNOWN -> ExecutionPlanLevel.UNKNOWN;
		};
	}

	private ExecutionPlanReason reason(
			ExecutionPermissionIntegrationResult executionPermissionIntegration,
			String executionPlanIdentifier,
			String executionSequence,
			boolean rollbackPlanPresent,
			boolean verificationPlanPresent,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		if (paymentSafetyUncertainty) {
			return ExecutionPlanReason.PAYMENT_SAFETY_UNCERTAINTY;
		}
		if (lifecycleRisk == OperationalUncertainty.CRITICAL) {
			return ExecutionPlanReason.CRITICAL_LIFECYCLE_RISK;
		}
		if (missingText(executionPlanIdentifier)) {
			return ExecutionPlanReason.MISSING_EXECUTION_PLAN_IDENTIFIER;
		}
		if (missingText(executionSequence)) {
			return ExecutionPlanReason.MISSING_EXECUTION_SEQUENCE;
		}
		if (!rollbackPlanPresent) {
			return ExecutionPlanReason.MISSING_ROLLBACK_PLAN;
		}
		if (!verificationPlanPresent) {
			return ExecutionPlanReason.MISSING_VERIFICATION_PLAN;
		}

		return switch (executionPermissionIntegration.status()) {
			case EXECUTION_PERMISSION_READY -> ExecutionPlanReason.EXECUTION_PERMISSION_READY;
			case PARTIAL_EXECUTION_PERMISSION -> ExecutionPlanReason.PARTIAL_EXECUTION_PERMISSION;
			case NOT_READY -> ExecutionPlanReason.NOT_READY_EXECUTION_PERMISSION;
			case UNRELIABLE -> ExecutionPlanReason.UNRELIABLE_EXECUTION_PERMISSION;
			case BLOCKED -> ExecutionPlanReason.BLOCKED_EXECUTION_PERMISSION;
			case UNKNOWN -> ExecutionPlanReason.UNKNOWN;
		};
	}

	private ExecutionPlanScope scope(
			ExecutionPermissionIntegrationResult executionPermissionIntegration,
			String executionPlanIdentifier,
			String executionSequence,
			boolean rollbackPlanPresent,
			boolean verificationPlanPresent,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		if (paymentSafetyUncertainty) {
			return ExecutionPlanScope.PAYMENT_SAFETY;
		}
		if (lifecycleRisk == OperationalUncertainty.CRITICAL) {
			return ExecutionPlanScope.LIFECYCLE_RISK;
		}
		if (missingText(executionPlanIdentifier)) {
			return ExecutionPlanScope.EXECUTION_PLAN;
		}
		if (missingText(executionSequence)) {
			return ExecutionPlanScope.EXECUTION_SEQUENCE;
		}
		if (!rollbackPlanPresent) {
			return ExecutionPlanScope.ROLLBACK_PLAN;
		}
		if (!verificationPlanPresent) {
			return ExecutionPlanScope.VERIFICATION_PLAN;
		}

		return ExecutionPlanScope.EXECUTION_PLAN;
	}

	private boolean missingText(String value) {
		return value == null || value.isBlank();
	}
}
