package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public class VerificationReliabilityEvaluator {

	public VerificationReliability evaluate(
			ApprovalReliability approvalReliability,
			boolean verificationEvidenceRequirementPresent
	) {
		Objects.requireNonNull(
				approvalReliability,
				"approvalReliability must not be null"
		);

		return new VerificationReliability(
				level(approvalReliability, verificationEvidenceRequirementPresent),
				reason(approvalReliability, verificationEvidenceRequirementPresent),
				scope(approvalReliability, verificationEvidenceRequirementPresent),
				approvalReliability,
				verificationEvidenceRequirementPresent
		);
	}

	private VerificationReliabilityLevel level(
			ApprovalReliability approvalReliability,
			boolean verificationEvidenceRequirementPresent
	) {
		if (missingVerificationBinding(approvalReliability)) {
			return VerificationReliabilityLevel.BLOCKED;
		}
		if (!verificationEvidenceRequirementPresent) {
			return VerificationReliabilityLevel.BLOCKED;
		}
		if (missingRollbackBinding(approvalReliability)) {
			return VerificationReliabilityLevel.BLOCKED;
		}
		if (approvalReliability.level() == ApprovalReliabilityLevel.BLOCKED) {
			return VerificationReliabilityLevel.BLOCKED;
		}
		if (approvalReliability.level() == ApprovalReliabilityLevel.UNRELIABLE) {
			return VerificationReliabilityLevel.UNRELIABLE;
		}
		if (contradictoryApproval(approvalReliability)
				|| contradictoryRecommendation(approvalReliability)) {
			return VerificationReliabilityLevel.LOW;
		}
		if (paymentSafetyUncertainty(approvalReliability)) {
			return VerificationReliabilityLevel.LOW;
		}
		if (approvalReliability.level() == ApprovalReliabilityLevel.LOW) {
			return VerificationReliabilityLevel.LOW;
		}
		if (highVerificationReliability(approvalReliability, verificationEvidenceRequirementPresent)) {
			return VerificationReliabilityLevel.HIGH;
		}
		if (approvalReliability.level() == ApprovalReliabilityLevel.MEDIUM
				|| restrictedBindings(approvalReliability)) {
			return VerificationReliabilityLevel.MEDIUM;
		}
		return VerificationReliabilityLevel.UNKNOWN;
	}

	private VerificationReliabilityReason reason(
			ApprovalReliability approvalReliability,
			boolean verificationEvidenceRequirementPresent
	) {
		if (missingVerificationBinding(approvalReliability)) {
			return VerificationReliabilityReason.MISSING_VERIFICATION_BINDING;
		}
		if (!verificationEvidenceRequirementPresent) {
			return VerificationReliabilityReason.MISSING_VERIFICATION_EVIDENCE_REQUIREMENT;
		}
		if (missingRollbackBinding(approvalReliability)) {
			return VerificationReliabilityReason.MISSING_ROLLBACK_BINDING;
		}
		if (approvalReliability.level() == ApprovalReliabilityLevel.BLOCKED) {
			return VerificationReliabilityReason.BLOCKED_APPROVAL;
		}
		if (approvalReliability.level() == ApprovalReliabilityLevel.UNRELIABLE) {
			return VerificationReliabilityReason.UNRELIABLE_APPROVAL;
		}
		if (contradictoryApproval(approvalReliability)) {
			return VerificationReliabilityReason.CONTRADICTORY_APPROVAL;
		}
		if (contradictoryRecommendation(approvalReliability)) {
			return VerificationReliabilityReason.CONTRADICTORY_RECOMMENDATION;
		}
		if (paymentSafetyUncertainty(approvalReliability)) {
			return VerificationReliabilityReason.PAYMENT_SAFETY_UNCERTAINTY;
		}
		if (approvalReliability.level() == ApprovalReliabilityLevel.LOW) {
			return VerificationReliabilityReason.LOW_APPROVAL_RELIABILITY;
		}
		if (highVerificationReliability(approvalReliability, verificationEvidenceRequirementPresent)) {
			return VerificationReliabilityReason.HIGH_APPROVAL_RELIABILITY;
		}
		return VerificationReliabilityReason.UNKNOWN;
	}

	private VerificationReliabilityScope scope(
			ApprovalReliability approvalReliability,
			boolean verificationEvidenceRequirementPresent
	) {
		if (missingVerificationBinding(approvalReliability)) {
			return VerificationReliabilityScope.VERIFICATION;
		}
		if (!verificationEvidenceRequirementPresent) {
			return VerificationReliabilityScope.VERIFICATION_EVIDENCE;
		}
		if (missingRollbackBinding(approvalReliability)) {
			return VerificationReliabilityScope.ROLLBACK_BOUNDARY;
		}
		if (approvalReliability.level() == ApprovalReliabilityLevel.BLOCKED
				|| approvalReliability.level() == ApprovalReliabilityLevel.UNRELIABLE) {
			return VerificationReliabilityScope.APPROVAL;
		}
		if (paymentSafetyUncertainty(approvalReliability)) {
			return VerificationReliabilityScope.PAYMENT_SAFETY;
		}
		if (contradictoryApproval(approvalReliability)
				|| contradictoryRecommendation(approvalReliability)) {
			return VerificationReliabilityScope.LIFECYCLE;
		}
		if (approvalReliability.level() == ApprovalReliabilityLevel.LOW
				|| approvalReliability.level() == ApprovalReliabilityLevel.MEDIUM) {
			return VerificationReliabilityScope.OPERATOR_VIEW;
		}
		return VerificationReliabilityScope.APPROVAL;
	}

	private boolean highVerificationReliability(
			ApprovalReliability approvalReliability,
			boolean verificationEvidenceRequirementPresent
	) {
		return approvalReliability.level() == ApprovalReliabilityLevel.HIGH
				&& !missingVerificationBinding(approvalReliability)
				&& verificationEvidenceRequirementPresent
				&& !missingRollbackBinding(approvalReliability)
				&& !paymentSafetyUncertainty(approvalReliability)
				&& !contradictoryApproval(approvalReliability)
				&& !contradictoryRecommendation(approvalReliability);
	}

	private boolean missingVerificationBinding(ApprovalReliability approvalReliability) {
		RollbackVerificationBindingDecision decision = approvalReliability
				.recommendationReliability()
				.decisionReliability()
				.rollbackVerificationBindingDecision();
		return decision == null
				|| decision.rejectionReason()
				== RollbackVerificationBindingRejectionReason.MISSING_VERIFICATION_REFERENCE;
	}

	private boolean missingRollbackBinding(ApprovalReliability approvalReliability) {
		RollbackVerificationBindingDecision decision = approvalReliability
				.recommendationReliability()
				.decisionReliability()
				.rollbackVerificationBindingDecision();
		return decision == null
				|| decision.rejectionReason()
				== RollbackVerificationBindingRejectionReason.MISSING_ROLLBACK_REFERENCE;
	}

	private boolean restrictedBindings(ApprovalReliability approvalReliability) {
		RollbackVerificationBindingDecision decision = approvalReliability
				.recommendationReliability()
				.decisionReliability()
				.rollbackVerificationBindingDecision();
		return decision != null
				&& decision.status() == RollbackVerificationBindingStatus.RESTRICTED;
	}

	private boolean paymentSafetyUncertainty(ApprovalReliability approvalReliability) {
		return approvalReliability.reason() == ApprovalReliabilityReason.PAYMENT_SAFETY_UNCERTAINTY
				|| approvalReliability.recommendationReliability().reason()
				== RecommendationReliabilityReason.PAYMENT_SAFETY_UNCERTAINTY;
	}

	private boolean contradictoryApproval(ApprovalReliability approvalReliability) {
		return approvalReliability.reason()
				== ApprovalReliabilityReason.CONTRADICTORY_RECOMMENDATION;
	}

	private boolean contradictoryRecommendation(ApprovalReliability approvalReliability) {
		return approvalReliability.recommendationReliability().reason()
				== RecommendationReliabilityReason.CONTRADICTORY_DECISION
				&& approvalReliability.reason() != ApprovalReliabilityReason.CONTRADICTORY_RECOMMENDATION;
	}
}
