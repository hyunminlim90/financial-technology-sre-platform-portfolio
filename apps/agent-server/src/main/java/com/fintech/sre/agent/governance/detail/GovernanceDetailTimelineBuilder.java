package com.fintech.sre.agent.governance.detail;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.incident.lifecycle.IncidentLifecycleRecord;
import com.fintech.sre.agent.learning.application.KnowledgeUpdateApplicationRecord;
import com.fintech.sre.agent.learning.candidate.LearningCandidateRecord;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanRecord;
import com.fintech.sre.agent.learning.promotion.KnowledgePromotionReviewRecord;
import com.fintech.sre.agent.postmortem.draft.PostmortemDraftRecord;
import com.fintech.sre.agent.postmortem.review.PostmortemReviewRecord;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalRecord;
import com.fintech.sre.agent.recommendation.execution.RecommendationExecutionPlan;
import com.fintech.sre.agent.recommendation.execution.result.HumanExecutionResultRecord;
import com.fintech.sre.agent.recommendation.persistence.RecommendationRecord;
import com.fintech.sre.agent.recommendation.verification.VerificationResultRecord;

@Component
public class GovernanceDetailTimelineBuilder {

	private final GovernanceDetailSanitizer sanitizer;

	public GovernanceDetailTimelineBuilder(
			GovernanceDetailSanitizer sanitizer
	) {
		this.sanitizer = sanitizer;
	}

	public List<GovernanceDetailTimelineItem> buildIncidentTimeline(
			List<IncidentLifecycleRecord> lifecycles,
			List<RecommendationRecord> recommendations,
			List<RecommendationApprovalRecord> approvals,
			List<RecommendationExecutionPlan> executionPlans,
			List<HumanExecutionResultRecord> humanExecutionResults,
			List<VerificationResultRecord> verifications,
			List<PostmortemDraftRecord> postmortemDrafts,
			List<PostmortemReviewRecord> postmortemReviews,
			List<LearningCandidateRecord> learningCandidates,
			List<KnowledgeUpdateApplicationRecord> knowledgeUpdates
	) {
		return Stream.of(
				lifecycles == null ? Stream.<GovernanceDetailTimelineItem>empty() : lifecycles.stream()
						.map(this::incidentTransitioned),
				recommendations == null ? Stream.<GovernanceDetailTimelineItem>empty() : recommendations.stream()
						.map(this::recommendationCreated),
				approvals == null ? Stream.<GovernanceDetailTimelineItem>empty() : approvals.stream()
						.map(this::approvalDecided),
				executionPlans == null ? Stream.<GovernanceDetailTimelineItem>empty() : executionPlans.stream()
						.map(this::executionPlanCreated),
				humanExecutionResults == null ? Stream.<GovernanceDetailTimelineItem>empty() : humanExecutionResults.stream()
						.map(this::humanExecutionRecorded),
				verifications == null ? Stream.<GovernanceDetailTimelineItem>empty() : verifications.stream()
						.map(this::verificationRecorded),
				postmortemDrafts == null ? Stream.<GovernanceDetailTimelineItem>empty() : postmortemDrafts.stream()
						.map(this::postmortemDraftCreated),
				postmortemReviews == null ? Stream.<GovernanceDetailTimelineItem>empty() : postmortemReviews.stream()
						.map(this::postmortemReviewed),
				learningCandidates == null ? Stream.<GovernanceDetailTimelineItem>empty() : learningCandidates.stream()
						.map(this::learningCandidateCreated),
				knowledgeUpdates == null ? Stream.<GovernanceDetailTimelineItem>empty() : knowledgeUpdates.stream()
						.map(this::knowledgeUpdated)
		).flatMap(stream -> stream)
				.sorted(Comparator.comparing(GovernanceDetailTimelineItem::occurredAt))
				.toList();
	}

	public List<GovernanceDetailTimelineItem> buildRecommendationTimeline(
			RecommendationRecord recommendation,
			List<RecommendationApprovalRecord> approvals,
			List<RecommendationExecutionPlan> executionPlans,
			List<HumanExecutionResultRecord> humanExecutionResults,
			List<VerificationResultRecord> verifications
	) {
		return Stream.of(
				recommendation == null
						? Stream.<GovernanceDetailTimelineItem>empty()
						: Stream.of(recommendationCreated(recommendation)),
				approvals == null ? Stream.<GovernanceDetailTimelineItem>empty() : approvals.stream()
						.map(this::approvalDecided),
				executionPlans == null ? Stream.<GovernanceDetailTimelineItem>empty() : executionPlans.stream()
						.map(this::executionPlanCreated),
				humanExecutionResults == null ? Stream.<GovernanceDetailTimelineItem>empty() : humanExecutionResults.stream()
						.map(this::humanExecutionRecorded),
				verifications == null ? Stream.<GovernanceDetailTimelineItem>empty() : verifications.stream()
						.map(this::verificationRecorded)
		).flatMap(stream -> stream)
				.sorted(Comparator.comparing(GovernanceDetailTimelineItem::occurredAt))
				.toList();
	}

	public List<GovernanceDetailTimelineItem> buildLearningTimeline(
			LearningCandidateRecord learningCandidate,
			List<KnowledgePromotionReviewRecord> promotionReviews,
			List<KnowledgePromotionPlanRecord> promotionPlans,
			List<KnowledgeUpdateApplicationRecord> knowledgeUpdates
	) {
		return Stream.of(
				learningCandidate == null
						? Stream.<GovernanceDetailTimelineItem>empty()
						: Stream.of(learningCandidateCreated(learningCandidate)),
				promotionReviews == null ? Stream.<GovernanceDetailTimelineItem>empty() : promotionReviews.stream()
						.map(this::promotionReviewed),
				promotionPlans == null ? Stream.<GovernanceDetailTimelineItem>empty() : promotionPlans.stream()
						.map(this::promotionPlanCreated),
				knowledgeUpdates == null ? Stream.<GovernanceDetailTimelineItem>empty() : knowledgeUpdates.stream()
						.map(this::knowledgeUpdated)
		).flatMap(stream -> stream)
				.sorted(Comparator.comparing(GovernanceDetailTimelineItem::occurredAt))
				.toList();
	}

	private GovernanceDetailTimelineItem recommendationCreated(
			RecommendationRecord record
	) {
		return item(
				record.generatedAt(),
				"RECOMMENDATION_CREATED",
				record.recommendationRecordId(),
				record.policyDecision(),
				"Recommendation created",
				record.service() + " / " + record.domain()
		);
	}

	private GovernanceDetailTimelineItem approvalDecided(
			RecommendationApprovalRecord record
	) {
		return item(
				record.decidedAt(),
				"APPROVAL_DECIDED",
				record.approvalId(),
				record.status(),
				"Approval decided",
				record.reason()
		);
	}

	private GovernanceDetailTimelineItem executionPlanCreated(
			RecommendationExecutionPlan record
	) {
		return item(
				record.createdAt(),
				"EXECUTION_PLAN_CREATED",
				record.executionPlanId(),
				record.status(),
				"Execution plan created",
				record.reason()
		);
	}

	private GovernanceDetailTimelineItem humanExecutionRecorded(
			HumanExecutionResultRecord record
	) {
		return item(
				record.recordedAt(),
				"HUMAN_EXECUTION_RECORDED",
				record.executionResultId(),
				record.status(),
				"Human execution recorded",
				record.summary()
		);
	}

	private GovernanceDetailTimelineItem verificationRecorded(
			VerificationResultRecord record
	) {
		return item(
				record.verifiedAt(),
				"VERIFICATION_RECORDED",
				record.verificationResultId(),
				record.status(),
				"Verification recorded",
				record.summary()
		);
	}

	private GovernanceDetailTimelineItem incidentTransitioned(
			IncidentLifecycleRecord record
	) {
		return item(
				record.transitionedAt(),
				"INCIDENT_TRANSITIONED",
				record.incidentLifecycleId(),
				record.currentStatus(),
				"Incident transitioned",
				record.summary()
		);
	}

	private GovernanceDetailTimelineItem postmortemDraftCreated(
			PostmortemDraftRecord record
	) {
		return item(
				record.createdAt(),
				"POSTMORTEM_DRAFT_CREATED",
				record.postmortemDraftId(),
				record.status(),
				"Postmortem draft created",
				record.summary()
		);
	}

	private GovernanceDetailTimelineItem postmortemReviewed(
			PostmortemReviewRecord record
	) {
		return item(
				record.reviewedAt(),
				"POSTMORTEM_REVIEWED",
				record.postmortemReviewId(),
				record.status(),
				"Postmortem reviewed",
				record.reviewSummary()
		);
	}

	private GovernanceDetailTimelineItem learningCandidateCreated(
			LearningCandidateRecord record
	) {
		return item(
				record.createdAt(),
				"LEARNING_CANDIDATE_CREATED",
				record.learningCandidateId(),
				record.status(),
				"Learning candidate created",
				record.summary()
		);
	}

	private GovernanceDetailTimelineItem promotionReviewed(
			KnowledgePromotionReviewRecord record
	) {
		return item(
				record.reviewedAt(),
				"PROMOTION_REVIEWED",
				record.promotionReviewId(),
				record.status(),
				"Promotion reviewed",
				record.reviewSummary()
		);
	}

	private GovernanceDetailTimelineItem promotionPlanCreated(
			KnowledgePromotionPlanRecord record
	) {
		return item(
				record.createdAt(),
				"PROMOTION_PLAN_CREATED",
				record.promotionPlanId(),
				record.status(),
				"Promotion plan created",
				record.summary()
		);
	}

	private GovernanceDetailTimelineItem knowledgeUpdated(
			KnowledgeUpdateApplicationRecord record
	) {
		return item(
				record.appliedAt(),
				"KNOWLEDGE_UPDATED",
				record.knowledgeUpdateApplicationId(),
				record.changeType(),
				"Knowledge update applied",
				record.filePath()
		);
	}

	private GovernanceDetailTimelineItem item(
			Instant occurredAt,
			String type,
			String recordId,
			Object status,
			String title,
			String summary
	) {
		return new GovernanceDetailTimelineItem(
				occurredAt,
				type,
				recordId,
				sanitizer.safeStatus(status),
				sanitizer.safeText(title),
				sanitizer.safeText(summary)
		);
	}
}
