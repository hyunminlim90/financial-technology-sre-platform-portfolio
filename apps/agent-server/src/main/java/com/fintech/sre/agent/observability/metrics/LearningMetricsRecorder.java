package com.fintech.sre.agent.observability.metrics;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.learning.application.KnowledgeUpdateApplicationRecord;
import com.fintech.sre.agent.learning.candidate.LearningCandidateRecord;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanRecord;
import com.fintech.sre.agent.learning.promotion.KnowledgePromotionReviewRecord;
import com.fintech.sre.agent.postmortem.draft.PostmortemDraftRecord;
import com.fintech.sre.agent.postmortem.review.PostmortemReviewRecord;

@Component
public class LearningMetricsRecorder {

	private final GovernanceMetricsRecorder recorder;

	public LearningMetricsRecorder(GovernanceMetricsRecorder recorder) {
		this.recorder = recorder;
	}

	public void recordPostmortemDraft(PostmortemDraftRecord record) {
		recorder.increment(
				GovernanceMetricName.POSTMORTEM_DRAFT_CREATED,
				Map.of("status", record.status().name())
		);
	}

	public void recordPostmortemReview(PostmortemReviewRecord record) {
		recorder.increment(
				GovernanceMetricName.POSTMORTEM_REVIEW_DECISION,
				Map.of("status", record.status().name())
		);
	}

	public void recordLearningCandidate(LearningCandidateRecord record) {
		recorder.increment(
				GovernanceMetricName.LEARNING_CANDIDATE_CREATED,
				Map.of(
						"type", record.type().name(),
						"status", record.status().name()
				)
		);
	}

	public void recordPromotionReview(KnowledgePromotionReviewRecord record) {
		recorder.increment(
				GovernanceMetricName.KNOWLEDGE_PROMOTION_REVIEW,
				Map.of("status", record.status().name())
		);
	}

	public void recordPromotionPlan(KnowledgePromotionPlanRecord record) {
		recorder.increment(
				GovernanceMetricName.KNOWLEDGE_PROMOTION_PLAN_CREATED,
				Map.of("status", record.status().name())
		);
	}

	public void recordKnowledgeUpdate(KnowledgeUpdateApplicationRecord record) {
		recorder.increment(
				GovernanceMetricName.KNOWLEDGE_UPDATE_APPLIED,
				Map.of(
						"knowledgeType", safe(record.knowledgeType()),
						"knowledgeLayer", record.knowledgeLayer() == null
								? "unknown"
								: record.knowledgeLayer().name(),
						"changeType", record.changeType() == null
								? "unknown"
								: record.changeType().name()
				)
		);
	}

	private String safe(String value) {
		return value == null || value.isBlank() ? "unknown" : value;
	}
}
