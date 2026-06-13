package com.fintech.sre.agent.runtime.readiness;

import java.util.Objects;

import com.fintech.sre.agent.runtime.reliability.HumanApprovalDecision;
import com.fintech.sre.agent.runtime.reliability.OperationalUncertainty;
import com.fintech.sre.agent.runtime.reliability.RollbackVerificationBindingDecision;
import com.fintech.sre.agent.runtime.reliability.RollbackVerificationBindingRejectionReason;

public class ActionAdmissionReadinessEvaluator {

	public ActionAdmissionReadiness evaluate(
			VerificationReadiness verificationReadiness,
			String actionType,
			String blastRadiusBoundary
	) {
		Objects.requireNonNull(
				verificationReadiness,
				"verificationReadiness must not be null"
		);

		return new ActionAdmissionReadiness(
				level(verificationReadiness, actionType, blastRadiusBoundary),
				reason(verificationReadiness, actionType, blastRadiusBoundary),
				scope(verificationReadiness, actionType, blastRadiusBoundary),
				verificationReadiness,
				actionType,
				blastRadiusBoundary
		);
	}

	private ActionAdmissionReadinessLevel level(
			VerificationReadiness verificationReadiness,
			String actionType,
			String blastRadiusBoundary
	) {
		if (paymentSafetyUncertainty(verificationReadiness)) {
			return ActionAdmissionReadinessLevel.BLOCKED;
		}
		if (lifecycleRiskCritical(verificationReadiness)) {
			return ActionAdmissionReadinessLevel.BLOCKED;
		}
		if (missingActionType(actionType)) {
			return ActionAdmissionReadinessLevel.BLOCKED;
		}
		if (missingBlastRadiusBoundary(blastRadiusBoundary)) {
			return ActionAdmissionReadinessLevel.BLOCKED;
		}
		if (missingRollbackBinding(verificationReadiness)) {
			return ActionAdmissionReadinessLevel.BLOCKED;
		}
		if (missingVerificationBinding(verificationReadiness)) {
			return ActionAdmissionReadinessLevel.BLOCKED;
		}
		if (missingHumanApprovalRequirement(verificationReadiness)) {
			return ActionAdmissionReadinessLevel.BLOCKED;
		}
		if (verificationReadiness.level() == VerificationReadinessLevel.BLOCKED) {
			return ActionAdmissionReadinessLevel.BLOCKED;
		}
		if (verificationReadiness.level() == VerificationReadinessLevel.UNRELIABLE) {
			return ActionAdmissionReadinessLevel.UNRELIABLE;
		}
		if (verificationReadiness.level() == VerificationReadinessLevel.NOT_READY) {
			return ActionAdmissionReadinessLevel.NOT_READY;
		}
		if (lifecycleUncertainty(verificationReadiness)) {
			return ActionAdmissionReadinessLevel.PARTIAL;
		}
		if (verificationReadiness.level() == VerificationReadinessLevel.PARTIAL) {
			return ActionAdmissionReadinessLevel.PARTIAL;
		}
		if (ready(verificationReadiness, actionType, blastRadiusBoundary)) {
			return ActionAdmissionReadinessLevel.READY;
		}
		return ActionAdmissionReadinessLevel.UNKNOWN;
	}

	private ActionAdmissionReadinessReason reason(
			VerificationReadiness verificationReadiness,
			String actionType,
			String blastRadiusBoundary
	) {
		if (paymentSafetyUncertainty(verificationReadiness)) {
			return ActionAdmissionReadinessReason.PAYMENT_SAFETY_UNCERTAINTY;
		}
		if (lifecycleRiskCritical(verificationReadiness)) {
			return ActionAdmissionReadinessReason.CRITICAL_LIFECYCLE_RISK;
		}
		if (missingActionType(actionType)) {
			return ActionAdmissionReadinessReason.MISSING_ACTION_TYPE;
		}
		if (missingBlastRadiusBoundary(blastRadiusBoundary)) {
			return ActionAdmissionReadinessReason.MISSING_BLAST_RADIUS_BOUNDARY;
		}
		if (missingRollbackBinding(verificationReadiness)) {
			return ActionAdmissionReadinessReason.MISSING_ROLLBACK_BINDING;
		}
		if (missingVerificationBinding(verificationReadiness)) {
			return ActionAdmissionReadinessReason.MISSING_VERIFICATION_BINDING;
		}
		if (missingHumanApprovalRequirement(verificationReadiness)) {
			return ActionAdmissionReadinessReason.MISSING_HUMAN_APPROVAL_REQUIREMENT;
		}
		if (verificationReadiness.level() == VerificationReadinessLevel.BLOCKED) {
			return ActionAdmissionReadinessReason.BLOCKED_VERIFICATION;
		}
		if (verificationReadiness.level() == VerificationReadinessLevel.UNRELIABLE) {
			return ActionAdmissionReadinessReason.UNRELIABLE_VERIFICATION;
		}
		if (verificationReadiness.level() == VerificationReadinessLevel.NOT_READY) {
			return ActionAdmissionReadinessReason.NOT_READY_VERIFICATION;
		}
		if (lifecycleUncertainty(verificationReadiness)) {
			return ActionAdmissionReadinessReason.LIFECYCLE_UNCERTAINTY;
		}
		if (verificationReadiness.level() == VerificationReadinessLevel.PARTIAL) {
			return ActionAdmissionReadinessReason.PARTIAL_VERIFICATION;
		}
		if (ready(verificationReadiness, actionType, blastRadiusBoundary)) {
			return ActionAdmissionReadinessReason.READY_VERIFICATION;
		}
		return ActionAdmissionReadinessReason.UNKNOWN;
	}

	private ActionAdmissionReadinessScope scope(
			VerificationReadiness verificationReadiness,
			String actionType,
			String blastRadiusBoundary
	) {
		if (paymentSafetyUncertainty(verificationReadiness)) {
			return ActionAdmissionReadinessScope.PAYMENT_SAFETY;
		}
		if (lifecycleRiskCritical(verificationReadiness)) {
			return ActionAdmissionReadinessScope.LIFECYCLE_RISK;
		}
		if (missingActionType(actionType)) {
			return ActionAdmissionReadinessScope.ACTION_TYPE;
		}
		if (missingBlastRadiusBoundary(blastRadiusBoundary)) {
			return ActionAdmissionReadinessScope.BLAST_RADIUS;
		}
		if (missingRollbackBinding(verificationReadiness)) {
			return ActionAdmissionReadinessScope.ROLLBACK_BOUNDARY;
		}
		if (missingVerificationBinding(verificationReadiness)) {
			return ActionAdmissionReadinessScope.VERIFICATION_BOUNDARY;
		}
		if (missingHumanApprovalRequirement(verificationReadiness)) {
			return ActionAdmissionReadinessScope.HUMAN_APPROVAL;
		}
		if (verificationReadiness.level() == VerificationReadinessLevel.BLOCKED
				|| verificationReadiness.level() == VerificationReadinessLevel.UNRELIABLE) {
			return ActionAdmissionReadinessScope.VERIFICATION_READINESS;
		}
		if (lifecycleUncertainty(verificationReadiness)) {
			return ActionAdmissionReadinessScope.LIFECYCLE_UNCERTAINTY;
		}
		if (verificationReadiness.level() == VerificationReadinessLevel.NOT_READY
				|| verificationReadiness.level() == VerificationReadinessLevel.PARTIAL) {
			return ActionAdmissionReadinessScope.OPERATOR_VIEW;
		}
		return ActionAdmissionReadinessScope.RUNTIME_READINESS;
	}

	private boolean ready(
			VerificationReadiness verificationReadiness,
			String actionType,
			String blastRadiusBoundary
	) {
		return verificationReadiness.level() == VerificationReadinessLevel.READY
				&& !missingActionType(actionType)
				&& !missingBlastRadiusBoundary(blastRadiusBoundary)
				&& !missingRollbackBinding(verificationReadiness)
				&& !missingVerificationBinding(verificationReadiness)
				&& !missingHumanApprovalRequirement(verificationReadiness)
				&& !lifecycleRiskCritical(verificationReadiness)
				&& !paymentSafetyUncertainty(verificationReadiness)
				&& !lifecycleUncertainty(verificationReadiness);
	}

	private boolean missingActionType(String actionType) {
		return actionType == null || actionType.isBlank();
	}

	private boolean missingBlastRadiusBoundary(String blastRadiusBoundary) {
		return blastRadiusBoundary == null || blastRadiusBoundary.isBlank();
	}

	private boolean lifecycleRiskCritical(VerificationReadiness verificationReadiness) {
		return verificationReadiness.approvalReadiness()
				.recommendationReadiness()
				.lifecycleRisk() == OperationalUncertainty.CRITICAL;
	}

	private boolean paymentSafetyUncertainty(VerificationReadiness verificationReadiness) {
		return verificationReadiness.approvalReadiness()
				.recommendationReadiness()
				.reason() == RecommendationReadinessReason.PAYMENT_SAFETY_UNCERTAINTY;
	}

	private boolean lifecycleUncertainty(VerificationReadiness verificationReadiness) {
		return verificationReadiness.approvalReadiness()
				.recommendationReadiness()
				.lifecycleUncertaintyDetected()
				|| verificationReadiness.approvalReadiness()
				.recommendationReadiness()
				.reason() == RecommendationReadinessReason.LIFECYCLE_UNCERTAINTY;
	}

	private boolean missingRollbackBinding(VerificationReadiness verificationReadiness) {
		RollbackVerificationBindingDecision decision = verificationReadiness
				.approvalReadiness()
				.recommendationReadiness()
				.recommendationReliability()
				.decisionReliability()
				.rollbackVerificationBindingDecision();
		return decision == null
				|| decision.rejectionReason()
				== RollbackVerificationBindingRejectionReason.MISSING_ROLLBACK_REFERENCE;
	}

	private boolean missingVerificationBinding(VerificationReadiness verificationReadiness) {
		RollbackVerificationBindingDecision decision = verificationReadiness
				.approvalReadiness()
				.recommendationReadiness()
				.recommendationReliability()
				.decisionReliability()
				.rollbackVerificationBindingDecision();
		return decision == null
				|| decision.rejectionReason()
				== RollbackVerificationBindingRejectionReason.MISSING_VERIFICATION_REFERENCE;
	}

	private boolean missingHumanApprovalRequirement(
			VerificationReadiness verificationReadiness
	) {
		HumanApprovalDecision decision = verificationReadiness
				.approvalReadiness()
				.recommendationReadiness()
				.recommendationReliability()
				.humanApprovalDecision();
		return decision == null || !decision.approvalRequired();
	}
}
