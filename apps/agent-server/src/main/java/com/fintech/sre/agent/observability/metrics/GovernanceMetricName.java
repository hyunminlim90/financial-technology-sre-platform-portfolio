package com.fintech.sre.agent.observability.metrics;

public final class GovernanceMetricName {

	private GovernanceMetricName() {
	}

	public static final String RECOMMENDATION_CREATED =
			"fin_sre_recommendation_created_total";

	public static final String RECOMMENDATION_APPROVAL_DECISION =
			"fin_sre_recommendation_approval_decision_total";

	public static final String EXECUTION_PLAN_CREATED =
			"fin_sre_execution_plan_created_total";

	public static final String HUMAN_EXECUTION_RESULT =
			"fin_sre_human_execution_result_total";

	public static final String VERIFICATION_RESULT =
			"fin_sre_verification_result_total";

	public static final String INCIDENT_LIFECYCLE_TRANSITION =
			"fin_sre_incident_lifecycle_transition_total";

	public static final String POSTMORTEM_DRAFT_CREATED =
			"fin_sre_postmortem_draft_created_total";

	public static final String POSTMORTEM_REVIEW_DECISION =
			"fin_sre_postmortem_review_decision_total";

	public static final String LEARNING_CANDIDATE_CREATED =
			"fin_sre_learning_candidate_created_total";

	public static final String KNOWLEDGE_PROMOTION_REVIEW =
			"fin_sre_knowledge_promotion_review_total";

	public static final String KNOWLEDGE_PROMOTION_PLAN_CREATED =
			"fin_sre_knowledge_promotion_plan_created_total";

	public static final String KNOWLEDGE_UPDATE_APPLIED =
			"fin_sre_knowledge_update_applied_total";
}
