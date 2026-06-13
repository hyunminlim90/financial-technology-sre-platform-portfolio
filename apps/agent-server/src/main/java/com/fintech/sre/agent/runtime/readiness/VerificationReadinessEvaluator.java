package com.fintech.sre.agent.runtime.readiness;

import java.util.Objects;

import com.fintech.sre.agent.runtime.reliability.OperationalUncertainty;
import com.fintech.sre.agent.runtime.reliability.RollbackVerificationBindingDecision;
import com.fintech.sre.agent.runtime.reliability.RollbackVerificationBindingRejectionReason;

public class VerificationReadinessEvaluator {

	public VerificationReadiness evaluate(
			ApprovalReadiness approvalReadiness,
			boolean verificationEvidenceRequirementPresent
	) {
		Objects.requireNonNull(
				approvalReadiness,
				"approvalReadiness must not be null"
		);

		return new VerificationReadiness(
				level(approvalReadiness, verificationEvidenceRequirementPresent),
				reason(approvalReadiness, verificationEvidenceRequirementPresent),
				scope(approvalReadiness, verificationEvidenceRequirementPresent),
				approvalReadiness,
				verificationEvidenceRequirementPresent
		);
	}

	private VerificationReadinessLevel level(
			ApprovalReadiness approvalReadiness,
			boolean verificationEvidenceRequirementPresent
	) {
		if (paymentSafetyUncertainty(approvalReadiness)) {
			return VerificationReadinessLevel.BLOCKED;
		}
		if (approvalReadiness.recommendationReadiness().lifecycleRisk()
				== OperationalUncertainty.CRITICAL) {
			return VerificationReadinessLevel.BLOCKED;
		}
		if (missingVerificationBinding(approvalReadiness)) {
			return VerificationReadinessLevel.BLOCKED;
		}
		if (!verificationEvidenceRequirementPresent) {
			return VerificationReadinessLevel.BLOCKED;
		}
		if (missingRollbackBinding(approvalReadiness)) {
			return VerificationReadinessLevel.BLOCKED;
		}
		if (approvalReadiness.level() == ApprovalReadinessLevel.BLOCKED) {
			return VerificationReadinessLevel.BLOCKED;
		}
		if (approvalReadiness.level() == ApprovalReadinessLevel.UNRELIABLE) {
			return VerificationReadinessLevel.UNRELIABLE;
		}
		if (approvalReadiness.level() == ApprovalReadinessLevel.NOT_READY) {
			return VerificationReadinessLevel.NOT_READY;
		}
		if (lifecycleUncertainty(approvalReadiness)) {
			return VerificationReadinessLevel.PARTIAL;
		}
		if (approvalReadiness.level() == ApprovalReadinessLevel.PARTIAL) {
			return VerificationReadinessLevel.PARTIAL;
		}
		if (ready(approvalReadiness, verificationEvidenceRequirementPresent)) {
			return VerificationReadinessLevel.READY;
		}
		return VerificationReadinessLevel.UNKNOWN;
	}

	private VerificationReadinessReason reason(
			ApprovalReadiness approvalReadiness,
			boolean verificationEvidenceRequirementPresent
	) {
		if (paymentSafetyUncertainty(approvalReadiness)) {
			return VerificationReadinessReason.PAYMENT_SAFETY_UNCERTAINTY;
		}
		if (approvalReadiness.recommendationReadiness().lifecycleRisk()
				== OperationalUncertainty.CRITICAL) {
			return VerificationReadinessReason.CRITICAL_LIFECYCLE_RISK;
		}
		if (missingVerificationBinding(approvalReadiness)) {
			return VerificationReadinessReason.MISSING_VERIFICATION_BINDING;
		}
		if (!verificationEvidenceRequirementPresent) {
			return VerificationReadinessReason.MISSING_VERIFICATION_EVIDENCE_REQUIREMENT;
		}
		if (missingRollbackBinding(approvalReadiness)) {
			return VerificationReadinessReason.MISSING_ROLLBACK_BINDING;
		}
		if (approvalReadiness.level() == ApprovalReadinessLevel.BLOCKED) {
			return VerificationReadinessReason.BLOCKED_APPROVAL;
		}
		if (approvalReadiness.level() == ApprovalReadinessLevel.UNRELIABLE) {
			return VerificationReadinessReason.UNRELIABLE_APPROVAL;
		}
		if (approvalReadiness.level() == ApprovalReadinessLevel.NOT_READY) {
			return VerificationReadinessReason.NOT_READY_APPROVAL;
		}
		if (lifecycleUncertainty(approvalReadiness)) {
			return VerificationReadinessReason.LIFECYCLE_UNCERTAINTY;
		}
		if (approvalReadiness.level() == ApprovalReadinessLevel.PARTIAL) {
			return VerificationReadinessReason.PARTIAL_APPROVAL;
		}
		if (ready(approvalReadiness, verificationEvidenceRequirementPresent)) {
			return VerificationReadinessReason.READY_APPROVAL;
		}
		return VerificationReadinessReason.UNKNOWN;
	}

	private VerificationReadinessScope scope(
			ApprovalReadiness approvalReadiness,
			boolean verificationEvidenceRequirementPresent
	) {
		if (paymentSafetyUncertainty(approvalReadiness)) {
			return VerificationReadinessScope.PAYMENT_SAFETY;
		}
		if (approvalReadiness.recommendationReadiness().lifecycleRisk()
				== OperationalUncertainty.CRITICAL) {
			return VerificationReadinessScope.LIFECYCLE_RISK;
		}
		if (missingVerificationBinding(approvalReadiness)) {
			return VerificationReadinessScope.VERIFICATION_BOUNDARY;
		}
		if (!verificationEvidenceRequirementPresent) {
			return VerificationReadinessScope.VERIFICATION_EVIDENCE;
		}
		if (missingRollbackBinding(approvalReadiness)) {
			return VerificationReadinessScope.ROLLBACK_BOUNDARY;
		}
		if (approvalReadiness.level() == ApprovalReadinessLevel.BLOCKED
				|| approvalReadiness.level() == ApprovalReadinessLevel.UNRELIABLE) {
			return VerificationReadinessScope.APPROVAL_READINESS;
		}
		if (lifecycleUncertainty(approvalReadiness)) {
			return VerificationReadinessScope.LIFECYCLE_UNCERTAINTY;
		}
		if (approvalReadiness.level() == ApprovalReadinessLevel.NOT_READY
				|| approvalReadiness.level() == ApprovalReadinessLevel.PARTIAL) {
			return VerificationReadinessScope.OPERATOR_VIEW;
		}
		return VerificationReadinessScope.RUNTIME_READINESS;
	}

	private boolean ready(
			ApprovalReadiness approvalReadiness,
			boolean verificationEvidenceRequirementPresent
	) {
		return approvalReadiness.level() == ApprovalReadinessLevel.READY
				&& !missingVerificationBinding(approvalReadiness)
				&& verificationEvidenceRequirementPresent
				&& !missingRollbackBinding(approvalReadiness)
				&& approvalReadiness.recommendationReadiness().lifecycleRisk()
				!= OperationalUncertainty.CRITICAL
				&& !paymentSafetyUncertainty(approvalReadiness)
				&& !lifecycleUncertainty(approvalReadiness);
	}

	private boolean paymentSafetyUncertainty(ApprovalReadiness approvalReadiness) {
		return approvalReadiness.recommendationReadiness().reason()
				== RecommendationReadinessReason.PAYMENT_SAFETY_UNCERTAINTY;
	}

	private boolean lifecycleUncertainty(ApprovalReadiness approvalReadiness) {
		return approvalReadiness.recommendationReadiness().lifecycleUncertaintyDetected()
				|| approvalReadiness.recommendationReadiness().reason()
				== RecommendationReadinessReason.LIFECYCLE_UNCERTAINTY;
	}

	private boolean missingVerificationBinding(ApprovalReadiness approvalReadiness) {
		RollbackVerificationBindingDecision decision = approvalReadiness
				.recommendationReadiness()
				.recommendationReliability()
				.decisionReliability()
				.rollbackVerificationBindingDecision();
		return decision == null
				|| decision.rejectionReason()
				== RollbackVerificationBindingRejectionReason.MISSING_VERIFICATION_REFERENCE;
	}

	private boolean missingRollbackBinding(ApprovalReadiness approvalReadiness) {
		RollbackVerificationBindingDecision decision = approvalReadiness
				.recommendationReadiness()
				.recommendationReliability()
				.decisionReliability()
				.rollbackVerificationBindingDecision();
		return decision == null
				|| decision.rejectionReason()
				== RollbackVerificationBindingRejectionReason.MISSING_ROLLBACK_REFERENCE;
	}
}
