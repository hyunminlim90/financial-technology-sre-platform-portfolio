package com.fintech.sre.agent.runtime.reliability;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ReliabilityRecommendationBoundary {

	public RecommendationEligibility evaluate(
			ReliabilityAssessmentResult assessmentResult,
			ReliabilityRiskClassification riskClassification,
			HumanApprovalDecision humanApprovalDecision
	) {
		Objects.requireNonNull(
				assessmentResult,
				"assessmentResult must not be null"
		);
		Objects.requireNonNull(
				riskClassification,
				"riskClassification must not be null"
		);
		Objects.requireNonNull(
				humanApprovalDecision,
				"humanApprovalDecision must not be null"
		);

		List<RecommendationRestriction> restrictions = new ArrayList<>();
		List<RecommendationBoundaryReason> reasons = new ArrayList<>();
		reasons.add(
				RecommendationBoundaryReason.ASSESSMENT_RESULT_IS_NOT_RECOMMENDATION
		);
		reasons.add(
				RecommendationBoundaryReason.AI_RECOMMENDATION_IS_ADVISORY_ONLY
		);
		restrictions.add(RecommendationRestriction.EXECUTION_AUTHORITY_PROHIBITED);

		if (noRecommendationScenario(assessmentResult)) {
			restrictions.add(RecommendationRestriction.NO_RECOMMENDATION_AVAILABLE);
			reasons.add(RecommendationBoundaryReason.NO_RECOMMENDATION_SCENARIO);
			return new RecommendationEligibility(
					false,
					RecommendationScope.NONE,
					false,
					false,
					restrictions,
					reasons
			);
		}

		boolean verificationRequirement =
				humanApprovalDecision.requirement().verificationReviewRequired();
		boolean rollbackRequirement =
				humanApprovalDecision.requirement().rollbackReviewRequired()
						|| riskClassification.level() == ReliabilityRiskLevel.CRITICAL
						|| assessmentResult.runtimeState() == RuntimeState.FAILED;

		if (assessmentResult.evidenceCorrelation().paymentSafetyUncertain()) {
			restrictions.add(RecommendationRestriction.UNSAFE_RECOMMENDATION_PROHIBITED);
			restrictions.add(RecommendationRestriction.HUMAN_APPROVAL_REQUIRED);
			reasons.add(
					RecommendationBoundaryReason
							.PAYMENT_SAFETY_UNCERTAINTY_RESTRICTS_UNSAFE_RECOMMENDATION
			);
			reasons.add(
					RecommendationBoundaryReason
							.HUMAN_APPROVAL_REQUIRED_PREVENTS_EXECUTION_AUTHORITY
			);
			if (verificationRequirement) {
				reasons.add(
						RecommendationBoundaryReason
								.VERIFICATION_REVIEW_MUST_ACCOMPANY_RECOMMENDATION
				);
			}
			return new RecommendationEligibility(
					true,
					RecommendationScope.HUMAN_APPROVAL_GATED_ADVISORY,
					verificationRequirement,
					rollbackRequirement,
					restrictions,
					reasons
			);
		}

		if (assessmentResult.evidenceCorrelation().contradictoryEvidence()) {
			restrictions.add(
					RecommendationRestriction.AUTOMATED_RECOMMENDATION_RESTRICTED
			);
			restrictions.add(RecommendationRestriction.HUMAN_APPROVAL_REQUIRED);
			reasons.add(
					RecommendationBoundaryReason
							.CONTRADICTORY_EVIDENCE_RESTRICTS_AUTOMATED_RECOMMENDATION
			);
			reasons.add(
					RecommendationBoundaryReason
							.HUMAN_APPROVAL_REQUIRED_PREVENTS_EXECUTION_AUTHORITY
			);
			if (verificationRequirement) {
				reasons.add(
						RecommendationBoundaryReason
								.VERIFICATION_REVIEW_MUST_ACCOMPANY_RECOMMENDATION
				);
			}
			return new RecommendationEligibility(
					true,
					RecommendationScope.HUMAN_APPROVAL_GATED_ADVISORY,
					verificationRequirement,
					rollbackRequirement,
					restrictions,
					reasons
			);
		}

		if (assessmentResult.runtimeState() == RuntimeState.FAILED
				|| riskClassification.level() == ReliabilityRiskLevel.CRITICAL) {
			restrictions.add(RecommendationRestriction.FAILED_STATE_RESTRICTED);
			restrictions.add(RecommendationRestriction.CRITICAL_RISK_RESTRICTED);
			restrictions.add(RecommendationRestriction.HUMAN_APPROVAL_REQUIRED);
			reasons.add(
					RecommendationBoundaryReason
							.FAILED_OR_CRITICAL_STATE_RESTRICTS_RECOMMENDATION
			);
			reasons.add(
					RecommendationBoundaryReason
							.HUMAN_APPROVAL_REQUIRED_PREVENTS_EXECUTION_AUTHORITY
			);
			reasons.add(
					RecommendationBoundaryReason
							.VERIFICATION_REVIEW_MUST_ACCOMPANY_RECOMMENDATION
			);
			reasons.add(
					RecommendationBoundaryReason
							.ROLLBACK_REVIEW_MUST_ACCOMPANY_RECOMMENDATION
			);
			return new RecommendationEligibility(
					true,
					RecommendationScope.ADVISORY_WITH_ROLLBACK_AND_VERIFICATION_REQUIREMENT,
					true,
					true,
					restrictions,
					reasons
			);
		}

		if (humanApprovalDecision.approvalRequired()) {
			restrictions.add(RecommendationRestriction.HUMAN_APPROVAL_REQUIRED);
			reasons.add(
					RecommendationBoundaryReason
							.HUMAN_APPROVAL_REQUIRED_PREVENTS_EXECUTION_AUTHORITY
			);
			if (verificationRequirement) {
				reasons.add(
						RecommendationBoundaryReason
								.VERIFICATION_REVIEW_MUST_ACCOMPANY_RECOMMENDATION
				);
			}
			return new RecommendationEligibility(
					true,
					RecommendationScope.HUMAN_APPROVAL_GATED_ADVISORY,
					verificationRequirement,
					rollbackRequirement,
					restrictions,
					reasons
			);
		}

		if (verificationRequirement || rollbackRequirement) {
			if (verificationRequirement) {
				reasons.add(
						RecommendationBoundaryReason
								.VERIFICATION_REVIEW_MUST_ACCOMPANY_RECOMMENDATION
				);
			}
			if (rollbackRequirement) {
				reasons.add(
						RecommendationBoundaryReason
								.ROLLBACK_REVIEW_MUST_ACCOMPANY_RECOMMENDATION
				);
			}
			RecommendationScope scope = rollbackRequirement
					? RecommendationScope.ADVISORY_WITH_ROLLBACK_AND_VERIFICATION_REQUIREMENT
					: RecommendationScope.ADVISORY_WITH_VERIFICATION_REQUIREMENT;
			return new RecommendationEligibility(
					true,
					scope,
					verificationRequirement,
					rollbackRequirement,
					restrictions,
					reasons
			);
		}

		return new RecommendationEligibility(
				true,
				RecommendationScope.ADVISORY_ONLY,
				false,
				false,
				restrictions,
				reasons
		);
	}

	private boolean noRecommendationScenario(
			ReliabilityAssessmentResult assessmentResult
	) {
		return assessmentResult.runtimeState() == RuntimeState.UNKNOWN
				|| assessmentResult.evidenceCorrelation().completeness()
						== EvidenceCompleteness.ABSENT;
	}
}
