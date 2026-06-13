package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public class ActionAdmissionReliabilityEvaluator {

	public ActionAdmissionReliability evaluate(
			VerificationReliability verificationReliability,
			String actionType,
			String blastRadiusBoundary
	) {
		Objects.requireNonNull(
				verificationReliability,
				"verificationReliability must not be null"
		);

		return new ActionAdmissionReliability(
				level(verificationReliability, actionType, blastRadiusBoundary),
				reason(verificationReliability, actionType, blastRadiusBoundary),
				scope(verificationReliability, actionType, blastRadiusBoundary),
				verificationReliability,
				actionType,
				blastRadiusBoundary
		);
	}

	private ActionAdmissionReliabilityLevel level(
			VerificationReliability verificationReliability,
			String actionType,
			String blastRadiusBoundary
	) {
		if (missingActionType(actionType)) {
			return ActionAdmissionReliabilityLevel.BLOCKED;
		}
		if (missingBlastRadiusBoundary(blastRadiusBoundary)) {
			return ActionAdmissionReliabilityLevel.BLOCKED;
		}
		if (missingRollbackBinding(verificationReliability)) {
			return ActionAdmissionReliabilityLevel.BLOCKED;
		}
		if (missingVerificationBinding(verificationReliability)) {
			return ActionAdmissionReliabilityLevel.BLOCKED;
		}
		if (missingHumanApprovalRequirement(verificationReliability)) {
			return ActionAdmissionReliabilityLevel.BLOCKED;
		}
		if (verificationReliability.level() == VerificationReliabilityLevel.BLOCKED) {
			return ActionAdmissionReliabilityLevel.BLOCKED;
		}
		if (verificationReliability.level() == VerificationReliabilityLevel.UNRELIABLE) {
			return ActionAdmissionReliabilityLevel.UNRELIABLE;
		}
		if (contradictoryVerification(verificationReliability)) {
			return ActionAdmissionReliabilityLevel.LOW;
		}
		if (paymentSafetyUncertainty(verificationReliability)) {
			return ActionAdmissionReliabilityLevel.LOW;
		}
		if (verificationReliability.level() == VerificationReliabilityLevel.LOW) {
			return ActionAdmissionReliabilityLevel.LOW;
		}
		if (highActionAdmissionReliability(
				verificationReliability,
				actionType,
				blastRadiusBoundary
		)) {
			return ActionAdmissionReliabilityLevel.HIGH;
		}
		if (verificationReliability.level() == VerificationReliabilityLevel.MEDIUM
				|| restrictedBindings(verificationReliability)) {
			return ActionAdmissionReliabilityLevel.MEDIUM;
		}
		return ActionAdmissionReliabilityLevel.UNKNOWN;
	}

	private ActionAdmissionReliabilityReason reason(
			VerificationReliability verificationReliability,
			String actionType,
			String blastRadiusBoundary
	) {
		if (missingActionType(actionType)) {
			return ActionAdmissionReliabilityReason.MISSING_ACTION_TYPE;
		}
		if (missingBlastRadiusBoundary(blastRadiusBoundary)) {
			return ActionAdmissionReliabilityReason.MISSING_BLAST_RADIUS_BOUNDARY;
		}
		if (missingRollbackBinding(verificationReliability)) {
			return ActionAdmissionReliabilityReason.MISSING_ROLLBACK_BINDING;
		}
		if (missingVerificationBinding(verificationReliability)) {
			return ActionAdmissionReliabilityReason.MISSING_VERIFICATION_BINDING;
		}
		if (missingHumanApprovalRequirement(verificationReliability)) {
			return ActionAdmissionReliabilityReason.MISSING_HUMAN_APPROVAL_REQUIREMENT;
		}
		if (verificationReliability.level() == VerificationReliabilityLevel.BLOCKED) {
			return ActionAdmissionReliabilityReason.BLOCKED_VERIFICATION;
		}
		if (verificationReliability.level() == VerificationReliabilityLevel.UNRELIABLE) {
			return ActionAdmissionReliabilityReason.UNRELIABLE_VERIFICATION;
		}
		if (contradictoryVerification(verificationReliability)) {
			return ActionAdmissionReliabilityReason.CONTRADICTORY_VERIFICATION;
		}
		if (paymentSafetyUncertainty(verificationReliability)) {
			return ActionAdmissionReliabilityReason.PAYMENT_SAFETY_UNCERTAINTY;
		}
		if (verificationReliability.level() == VerificationReliabilityLevel.LOW) {
			return ActionAdmissionReliabilityReason.LOW_VERIFICATION_RELIABILITY;
		}
		if (highActionAdmissionReliability(
				verificationReliability,
				actionType,
				blastRadiusBoundary
		)) {
			return ActionAdmissionReliabilityReason.HIGH_VERIFICATION_RELIABILITY;
		}
		return ActionAdmissionReliabilityReason.UNKNOWN;
	}

	private ActionAdmissionReliabilityScope scope(
			VerificationReliability verificationReliability,
			String actionType,
			String blastRadiusBoundary
	) {
		if (missingActionType(actionType)) {
			return ActionAdmissionReliabilityScope.ACTION_TYPE;
		}
		if (missingBlastRadiusBoundary(blastRadiusBoundary)) {
			return ActionAdmissionReliabilityScope.BLAST_RADIUS;
		}
		if (missingRollbackBinding(verificationReliability)) {
			return ActionAdmissionReliabilityScope.ROLLBACK_BOUNDARY;
		}
		if (missingVerificationBinding(verificationReliability)) {
			return ActionAdmissionReliabilityScope.VERIFICATION_BOUNDARY;
		}
		if (missingHumanApprovalRequirement(verificationReliability)) {
			return ActionAdmissionReliabilityScope.HUMAN_APPROVAL;
		}
		if (verificationReliability.level() == VerificationReliabilityLevel.BLOCKED
				|| verificationReliability.level() == VerificationReliabilityLevel.UNRELIABLE) {
			return ActionAdmissionReliabilityScope.VERIFICATION;
		}
		if (paymentSafetyUncertainty(verificationReliability)) {
			return ActionAdmissionReliabilityScope.PAYMENT_SAFETY;
		}
		if (contradictoryVerification(verificationReliability)) {
			return ActionAdmissionReliabilityScope.LIFECYCLE;
		}
		if (verificationReliability.level() == VerificationReliabilityLevel.LOW
				|| verificationReliability.level() == VerificationReliabilityLevel.MEDIUM) {
			return ActionAdmissionReliabilityScope.OPERATOR_VIEW;
		}
		return ActionAdmissionReliabilityScope.VERIFICATION;
	}

	private boolean highActionAdmissionReliability(
			VerificationReliability verificationReliability,
			String actionType,
			String blastRadiusBoundary
	) {
		return verificationReliability.level() == VerificationReliabilityLevel.HIGH
				&& !missingActionType(actionType)
				&& !missingBlastRadiusBoundary(blastRadiusBoundary)
				&& !missingRollbackBinding(verificationReliability)
				&& !missingVerificationBinding(verificationReliability)
				&& !missingHumanApprovalRequirement(verificationReliability)
				&& !paymentSafetyUncertainty(verificationReliability)
				&& !contradictoryVerification(verificationReliability);
	}

	private boolean missingActionType(String actionType) {
		return actionType == null || actionType.isBlank();
	}

	private boolean missingBlastRadiusBoundary(String blastRadiusBoundary) {
		return blastRadiusBoundary == null || blastRadiusBoundary.isBlank();
	}

	private boolean missingRollbackBinding(VerificationReliability verificationReliability) {
		RollbackVerificationBindingDecision decision = verificationReliability
				.approvalReliability()
				.recommendationReliability()
				.decisionReliability()
				.rollbackVerificationBindingDecision();
		return decision == null
				|| decision.rejectionReason()
				== RollbackVerificationBindingRejectionReason.MISSING_ROLLBACK_REFERENCE;
	}

	private boolean missingVerificationBinding(VerificationReliability verificationReliability) {
		RollbackVerificationBindingDecision decision = verificationReliability
				.approvalReliability()
				.recommendationReliability()
				.decisionReliability()
				.rollbackVerificationBindingDecision();
		return decision == null
				|| decision.rejectionReason()
				== RollbackVerificationBindingRejectionReason.MISSING_VERIFICATION_REFERENCE;
	}

	private boolean restrictedBindings(VerificationReliability verificationReliability) {
		RollbackVerificationBindingDecision decision = verificationReliability
				.approvalReliability()
				.recommendationReliability()
				.decisionReliability()
				.rollbackVerificationBindingDecision();
		return decision != null
				&& decision.status() == RollbackVerificationBindingStatus.RESTRICTED;
	}

	private boolean missingHumanApprovalRequirement(
			VerificationReliability verificationReliability
	) {
		HumanApprovalDecision decision = verificationReliability
				.approvalReliability()
				.recommendationReliability()
				.humanApprovalDecision();
		return decision == null
				|| !decision.approvalRequired()
				|| decision.requirement() == null;
	}

	private boolean paymentSafetyUncertainty(VerificationReliability verificationReliability) {
		return verificationReliability.reason()
				== VerificationReliabilityReason.PAYMENT_SAFETY_UNCERTAINTY
				|| verificationReliability.approvalReliability().reason()
				== ApprovalReliabilityReason.PAYMENT_SAFETY_UNCERTAINTY
				|| verificationReliability.approvalReliability()
				.recommendationReliability()
				.reason() == RecommendationReliabilityReason.PAYMENT_SAFETY_UNCERTAINTY;
	}

	private boolean contradictoryVerification(VerificationReliability verificationReliability) {
		return verificationReliability.reason()
				== VerificationReliabilityReason.CONTRADICTORY_APPROVAL
				|| verificationReliability.reason()
				== VerificationReliabilityReason.CONTRADICTORY_RECOMMENDATION;
	}
}
