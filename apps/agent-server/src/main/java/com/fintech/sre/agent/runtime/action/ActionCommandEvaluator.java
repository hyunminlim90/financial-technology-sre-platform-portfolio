package com.fintech.sre.agent.runtime.action;

import java.util.Objects;

import com.fintech.sre.agent.runtime.reliability.OperationalUncertainty;
import com.fintech.sre.agent.runtime.verification.VerificationRequestIntegrationResult;
import com.fintech.sre.agent.runtime.verification.VerificationRequestIntegrationStatus;

public class ActionCommandEvaluator {

	public ActionCommand evaluate(
			VerificationRequestIntegrationResult verificationRequestIntegration,
			String actionCommandIdentifier,
			String actionType,
			String targetLayer,
			String blastRadiusBoundary,
			boolean rollbackBindingPresent,
			boolean verificationBindingPresent,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		Objects.requireNonNull(
				verificationRequestIntegration,
				"verificationRequestIntegration must not be null"
		);
		Objects.requireNonNull(lifecycleRisk, "lifecycleRisk must not be null");

		return new ActionCommand(
				level(
						verificationRequestIntegration,
						actionCommandIdentifier,
						actionType,
						targetLayer,
						blastRadiusBoundary,
						rollbackBindingPresent,
						verificationBindingPresent,
						lifecycleRisk,
						paymentSafetyUncertainty
				),
				reason(
						verificationRequestIntegration,
						actionCommandIdentifier,
						actionType,
						targetLayer,
						blastRadiusBoundary,
						rollbackBindingPresent,
						verificationBindingPresent,
						lifecycleRisk,
						paymentSafetyUncertainty
				),
				scope(
						verificationRequestIntegration,
						actionCommandIdentifier,
						actionType,
						targetLayer,
						blastRadiusBoundary,
						rollbackBindingPresent,
						verificationBindingPresent,
						lifecycleRisk,
						paymentSafetyUncertainty
				),
				verificationRequestIntegration,
				actionCommandIdentifier,
				actionType,
				targetLayer,
				blastRadiusBoundary,
				rollbackBindingPresent,
				verificationBindingPresent,
				lifecycleRisk,
				paymentSafetyUncertainty
		);
	}

	private ActionCommandLevel level(
			VerificationRequestIntegrationResult verificationRequestIntegration,
			String actionCommandIdentifier,
			String actionType,
			String targetLayer,
			String blastRadiusBoundary,
			boolean rollbackBindingPresent,
			boolean verificationBindingPresent,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		if (paymentSafetyUncertainty) {
			return ActionCommandLevel.BLOCKED;
		}
		if (lifecycleRisk == OperationalUncertainty.CRITICAL) {
			return ActionCommandLevel.BLOCKED;
		}
		if (missingText(actionCommandIdentifier)) {
			return ActionCommandLevel.BLOCKED;
		}
		if (missingText(actionType)) {
			return ActionCommandLevel.BLOCKED;
		}
		if (missingText(targetLayer)) {
			return ActionCommandLevel.BLOCKED;
		}
		if (missingText(blastRadiusBoundary)) {
			return ActionCommandLevel.BLOCKED;
		}
		if (!rollbackBindingPresent) {
			return ActionCommandLevel.BLOCKED;
		}
		if (!verificationBindingPresent) {
			return ActionCommandLevel.BLOCKED;
		}
		return switch (verificationRequestIntegration.status()) {
			case VERIFICATION_REQUEST_READY -> ActionCommandLevel.ACTION_COMMAND_READY;
			case PARTIAL_VERIFICATION_REQUEST -> ActionCommandLevel.PARTIAL;
			case NOT_READY -> ActionCommandLevel.NOT_READY;
			case UNRELIABLE -> ActionCommandLevel.UNRELIABLE;
			case BLOCKED -> ActionCommandLevel.BLOCKED;
			case UNKNOWN -> ActionCommandLevel.UNKNOWN;
		};
	}

	private ActionCommandReason reason(
			VerificationRequestIntegrationResult verificationRequestIntegration,
			String actionCommandIdentifier,
			String actionType,
			String targetLayer,
			String blastRadiusBoundary,
			boolean rollbackBindingPresent,
			boolean verificationBindingPresent,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		if (paymentSafetyUncertainty) {
			return ActionCommandReason.PAYMENT_SAFETY_UNCERTAINTY;
		}
		if (lifecycleRisk == OperationalUncertainty.CRITICAL) {
			return ActionCommandReason.CRITICAL_LIFECYCLE_RISK;
		}
		if (missingText(actionCommandIdentifier)) {
			return ActionCommandReason.MISSING_ACTION_COMMAND_IDENTIFIER;
		}
		if (missingText(actionType)) {
			return ActionCommandReason.MISSING_ACTION_TYPE;
		}
		if (missingText(targetLayer)) {
			return ActionCommandReason.MISSING_TARGET_LAYER;
		}
		if (missingText(blastRadiusBoundary)) {
			return ActionCommandReason.MISSING_BLAST_RADIUS_BOUNDARY;
		}
		if (!rollbackBindingPresent) {
			return ActionCommandReason.MISSING_ROLLBACK_BINDING;
		}
		if (!verificationBindingPresent) {
			return ActionCommandReason.MISSING_VERIFICATION_BINDING;
		}
		return switch (verificationRequestIntegration.status()) {
			case VERIFICATION_REQUEST_READY -> ActionCommandReason.VERIFICATION_REQUEST_READY;
			case PARTIAL_VERIFICATION_REQUEST -> ActionCommandReason.PARTIAL_VERIFICATION_REQUEST;
			case NOT_READY -> ActionCommandReason.NOT_READY_VERIFICATION_REQUEST;
			case UNRELIABLE -> ActionCommandReason.UNRELIABLE_VERIFICATION_REQUEST;
			case BLOCKED -> ActionCommandReason.BLOCKED_VERIFICATION_REQUEST;
			case UNKNOWN -> ActionCommandReason.UNKNOWN;
		};
	}

	private ActionCommandScope scope(
			VerificationRequestIntegrationResult verificationRequestIntegration,
			String actionCommandIdentifier,
			String actionType,
			String targetLayer,
			String blastRadiusBoundary,
			boolean rollbackBindingPresent,
			boolean verificationBindingPresent,
			OperationalUncertainty lifecycleRisk,
			boolean paymentSafetyUncertainty
	) {
		if (paymentSafetyUncertainty) {
			return ActionCommandScope.PAYMENT_SAFETY;
		}
		if (lifecycleRisk == OperationalUncertainty.CRITICAL) {
			return ActionCommandScope.LIFECYCLE_RISK;
		}
		if (missingText(actionCommandIdentifier)) {
			return ActionCommandScope.ACTION_COMMAND;
		}
		if (missingText(actionType)) {
			return ActionCommandScope.ACTION_TYPE;
		}
		if (missingText(targetLayer)) {
			return ActionCommandScope.TARGET_LAYER;
		}
		if (missingText(blastRadiusBoundary)) {
			return ActionCommandScope.BLAST_RADIUS;
		}
		if (!rollbackBindingPresent) {
			return ActionCommandScope.ROLLBACK;
		}
		if (!verificationBindingPresent) {
			return ActionCommandScope.VERIFICATION;
		}
		return ActionCommandScope.ACTION_COMMAND;
	}

	private boolean missingText(String value) {
		return value == null || value.isBlank();
	}
}
