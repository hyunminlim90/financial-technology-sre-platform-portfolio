package com.fintech.sre.agent.runtime.recommendation;

import java.util.Objects;

public class RecommendationCandidateIntegration {

	public RecommendationCandidateIntegrationResult integrate(
			RecommendationCandidate recommendationCandidate
	) {
		Objects.requireNonNull(
				recommendationCandidate,
				"recommendationCandidate must not be null"
		);

		if (recommendationCandidate.level() == RecommendationCandidateLevel.BLOCKED) {
			return blockedResult(recommendationCandidate);
		}
		if (recommendationCandidate.level() == RecommendationCandidateLevel.UNRELIABLE) {
			return result(
					recommendationCandidate,
					RecommendationCandidateIntegrationStatus.UNRELIABLE,
					RecommendationCandidateIntegrationReason.UNRELIABLE_CANDIDATE,
					RecommendationCandidateIntegrationScope.RECOMMENDATION_CANDIDATE,
					false,
					false
			);
		}
		if (recommendationCandidate.level() == RecommendationCandidateLevel.NOT_READY) {
			return result(
					recommendationCandidate,
					RecommendationCandidateIntegrationStatus.NOT_READY,
					RecommendationCandidateIntegrationReason.NOT_READY_CANDIDATE,
					RecommendationCandidateIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
		}
		if (recommendationCandidate.level() == RecommendationCandidateLevel.PARTIAL) {
			return result(
					recommendationCandidate,
					RecommendationCandidateIntegrationStatus.PARTIAL_CANDIDATE,
					RecommendationCandidateIntegrationReason.PARTIAL_CANDIDATE,
					RecommendationCandidateIntegrationScope.OPERATOR_VIEW,
					false,
					false
			);
		}
		if (recommendationCandidate.level() == RecommendationCandidateLevel.ELIGIBLE) {
			return result(
					recommendationCandidate,
					RecommendationCandidateIntegrationStatus.GENERATION_READY,
					RecommendationCandidateIntegrationReason.ELIGIBLE_CANDIDATE,
					RecommendationCandidateIntegrationScope.RECOMMENDATION_CANDIDATE,
					true,
					true
			);
		}

		return result(
				recommendationCandidate,
				RecommendationCandidateIntegrationStatus.UNKNOWN,
				RecommendationCandidateIntegrationReason.UNKNOWN,
				RecommendationCandidateIntegrationScope.OPERATOR_VIEW,
				false,
				false
		);
	}

	public boolean readOnly() {
		return true;
	}

	public boolean recommendationGeneration() {
		return false;
	}

	public boolean recommendationMutation() {
		return false;
	}

	public boolean runbookSelection() {
		return false;
	}

	public boolean llmOrRagInvocation() {
		return false;
	}

	public boolean approvalRequest() {
		return false;
	}

	public boolean actionCommand() {
		return false;
	}

	public boolean executionPermission() {
		return false;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}

	private RecommendationCandidateIntegrationResult blockedResult(
			RecommendationCandidate recommendationCandidate
	) {
		if (recommendationCandidate.reason()
				== RecommendationCandidateReason.MISSING_SCENARIO_BINDING) {
			return result(
					recommendationCandidate,
					RecommendationCandidateIntegrationStatus.BLOCKED,
					RecommendationCandidateIntegrationReason.MISSING_SCENARIO_BINDING,
					RecommendationCandidateIntegrationScope.SCENARIO,
					false,
					false
			);
		}
		if (recommendationCandidate.reason()
				== RecommendationCandidateReason.MISSING_RUNBOOK_BINDING) {
			return result(
					recommendationCandidate,
					RecommendationCandidateIntegrationStatus.BLOCKED,
					RecommendationCandidateIntegrationReason.MISSING_RUNBOOK_BINDING,
					RecommendationCandidateIntegrationScope.RUNBOOK,
					false,
					false
			);
		}
		if (recommendationCandidate.reason()
				== RecommendationCandidateReason.MISSING_ROLLBACK_BINDING) {
			return result(
					recommendationCandidate,
					RecommendationCandidateIntegrationStatus.BLOCKED,
					RecommendationCandidateIntegrationReason.MISSING_ROLLBACK_BINDING,
					RecommendationCandidateIntegrationScope.ROLLBACK,
					false,
					false
			);
		}
		if (recommendationCandidate.reason()
				== RecommendationCandidateReason.MISSING_VERIFICATION_BINDING) {
			return result(
					recommendationCandidate,
					RecommendationCandidateIntegrationStatus.BLOCKED,
					RecommendationCandidateIntegrationReason.MISSING_VERIFICATION_BINDING,
					RecommendationCandidateIntegrationScope.VERIFICATION,
					false,
					false
			);
		}
		if (recommendationCandidate.reason()
				== RecommendationCandidateReason.PAYMENT_SAFETY_UNCERTAINTY) {
			return result(
					recommendationCandidate,
					RecommendationCandidateIntegrationStatus.BLOCKED,
					RecommendationCandidateIntegrationReason.PAYMENT_SAFETY_UNCERTAINTY,
					RecommendationCandidateIntegrationScope.PAYMENT_SAFETY,
					false,
					false
			);
		}
		if (recommendationCandidate.reason()
				== RecommendationCandidateReason.CRITICAL_LIFECYCLE_RISK) {
			return result(
					recommendationCandidate,
					RecommendationCandidateIntegrationStatus.BLOCKED,
					RecommendationCandidateIntegrationReason.CRITICAL_LIFECYCLE_RISK,
					RecommendationCandidateIntegrationScope.LIFECYCLE_RISK,
					false,
					false
			);
		}
		return result(
				recommendationCandidate,
				RecommendationCandidateIntegrationStatus.BLOCKED,
				RecommendationCandidateIntegrationReason.BLOCKED_CANDIDATE,
				RecommendationCandidateIntegrationScope.RECOMMENDATION_CANDIDATE,
				false,
				false
		);
	}

	private RecommendationCandidateIntegrationResult result(
			RecommendationCandidate recommendationCandidate,
			RecommendationCandidateIntegrationStatus status,
			RecommendationCandidateIntegrationReason reason,
			RecommendationCandidateIntegrationScope scope,
			boolean recommendationGenerationReadyView,
			boolean recommendationCertaintyAllowed
	) {
		return new RecommendationCandidateIntegrationResult(
				recommendationCandidate,
				status,
				reason,
				scope,
				recommendationGenerationReadyView,
				recommendationCertaintyAllowed
		);
	}
}
