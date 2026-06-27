package com.fintech.sre.agent.runtime.action;

import com.fintech.sre.agent.runtime.reliability.OperationalUncertainty;

public class ActionCommandIntegration {

	public ActionCommandIntegrationResult integrate(ActionCommand actionCommand) {
		if (actionCommand == null) {
			throw new NullPointerException("actionCommand must not be null");
		}

		if (actionCommand.paymentSafetyUncertainty()) {
			return result(
					actionCommand,
					ActionCommandIntegrationStatus.BLOCKED,
					ActionCommandIntegrationReason.PAYMENT_SAFETY_UNCERTAINTY,
					ActionCommandIntegrationScope.PAYMENT_SAFETY,
					false,
					false
			);
		}
		if (actionCommand.lifecycleRisk() == OperationalUncertainty.CRITICAL) {
			return result(
					actionCommand,
					ActionCommandIntegrationStatus.BLOCKED,
					ActionCommandIntegrationReason.CRITICAL_LIFECYCLE_RISK,
					ActionCommandIntegrationScope.LIFECYCLE_RISK,
					false,
					false
			);
		}
		if (missingActionCommandIdentifier(actionCommand)) {
			return result(
					actionCommand,
					ActionCommandIntegrationStatus.BLOCKED,
					ActionCommandIntegrationReason.MISSING_ACTION_COMMAND_IDENTIFIER,
					ActionCommandIntegrationScope.ACTION_COMMAND,
					false,
					false
			);
		}
		if (missingActionType(actionCommand)) {
			return result(
					actionCommand,
					ActionCommandIntegrationStatus.BLOCKED,
					ActionCommandIntegrationReason.MISSING_ACTION_TYPE,
					ActionCommandIntegrationScope.ACTION_TYPE,
					false,
					false
			);
		}
		if (missingTargetLayer(actionCommand)) {
			return result(
					actionCommand,
					ActionCommandIntegrationStatus.BLOCKED,
					ActionCommandIntegrationReason.MISSING_TARGET_LAYER,
					ActionCommandIntegrationScope.TARGET_LAYER,
					false,
					false
			);
		}
		if (missingBlastRadiusBoundary(actionCommand)) {
			return result(
					actionCommand,
					ActionCommandIntegrationStatus.BLOCKED,
					ActionCommandIntegrationReason.MISSING_BLAST_RADIUS_BOUNDARY,
					ActionCommandIntegrationScope.BLAST_RADIUS,
					false,
					false
			);
		}
		if (!actionCommand.rollbackBindingPresent()) {
			return result(
					actionCommand,
					ActionCommandIntegrationStatus.BLOCKED,
					ActionCommandIntegrationReason.MISSING_ROLLBACK_BINDING,
					ActionCommandIntegrationScope.ROLLBACK,
					false,
					false
			);
		}
		if (!actionCommand.verificationBindingPresent()) {
			return result(
					actionCommand,
					ActionCommandIntegrationStatus.BLOCKED,
					ActionCommandIntegrationReason.MISSING_VERIFICATION_BINDING,
					ActionCommandIntegrationScope.VERIFICATION,
					false,
					false
			);
		}

		return switch (actionCommand.level()) {
			case ACTION_COMMAND_READY -> result(
					actionCommand,
					ActionCommandIntegrationStatus.ACTION_COMMAND_CANDIDATE_READY,
					ActionCommandIntegrationReason.ACTION_COMMAND_READY,
					ActionCommandIntegrationScope.OPERATOR_VIEW,
					true,
					true
			);
			case PARTIAL -> result(
					actionCommand,
					ActionCommandIntegrationStatus.PARTIAL_ACTION_COMMAND,
					ActionCommandIntegrationReason.PARTIAL_ACTION_COMMAND,
					ActionCommandIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case NOT_READY -> result(
					actionCommand,
					ActionCommandIntegrationStatus.NOT_READY,
					ActionCommandIntegrationReason.NOT_READY_ACTION_COMMAND,
					ActionCommandIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case UNRELIABLE -> result(
					actionCommand,
					ActionCommandIntegrationStatus.UNRELIABLE,
					ActionCommandIntegrationReason.UNRELIABLE_ACTION_COMMAND,
					ActionCommandIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case BLOCKED -> result(
					actionCommand,
					ActionCommandIntegrationStatus.BLOCKED,
					ActionCommandIntegrationReason.BLOCKED_ACTION_COMMAND,
					ActionCommandIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
			case UNKNOWN -> result(
					actionCommand,
					ActionCommandIntegrationStatus.UNKNOWN,
					ActionCommandIntegrationReason.UNKNOWN,
					ActionCommandIntegrationScope.OPERATOR_VIEW,
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

	public boolean argoCdSync() {
		return false;
	}

	public boolean terraformApply() {
		return false;
	}

	public boolean executionPermission() {
		return false;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}

	private boolean missingActionCommandIdentifier(ActionCommand actionCommand) {
		return actionCommand.actionCommandIdentifier() == null
				|| actionCommand.actionCommandIdentifier().isBlank();
	}

	private boolean missingActionType(ActionCommand actionCommand) {
		return actionCommand.actionType() == null
				|| actionCommand.actionType().isBlank();
	}

	private boolean missingTargetLayer(ActionCommand actionCommand) {
		return actionCommand.targetLayer() == null
				|| actionCommand.targetLayer().isBlank();
	}

	private boolean missingBlastRadiusBoundary(ActionCommand actionCommand) {
		return actionCommand.blastRadiusBoundary() == null
				|| actionCommand.blastRadiusBoundary().isBlank();
	}

	private ActionCommandIntegrationResult result(
			ActionCommand actionCommand,
			ActionCommandIntegrationStatus status,
			ActionCommandIntegrationReason reason,
			ActionCommandIntegrationScope scope,
			boolean operatorFacingActionCommandCandidateVisible,
			boolean actionCommandCandidateCertaintyAllowed
	) {
		return new ActionCommandIntegrationResult(
				actionCommand,
				status,
				reason,
				scope,
				operatorFacingActionCommandCandidateVisible,
				actionCommandCandidateCertaintyAllowed
		);
	}
}
