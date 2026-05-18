package com.fintech.sre.agent.governance.detail;

import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class GovernanceDetailOverviewBuilder {

	private final GovernanceDetailSanitizer sanitizer;

	public GovernanceDetailOverviewBuilder(
			GovernanceDetailSanitizer sanitizer
	) {
		this.sanitizer = sanitizer;
	}

	public GovernanceDetailOverviewResponse fromIncident(
			GovernanceIncidentDetailResponse response
	) {
		return new GovernanceDetailOverviewResponse(
				GovernanceDetailType.INCIDENT,
				response.incidentId(),
				sanitizer.safeText(response.summary().title()),
				sanitizer.safeStatus(response.currentStatus()),
				new GovernanceDetailOverviewCounts(
						response.recommendations().size(),
						response.approvals().size(),
						response.executionPlans().size(),
						response.verifications().size(),
						response.postmortemDrafts().size() + response.postmortemReviews().size(),
						response.learningCandidates().size(),
						response.knowledgeUpdates().size()
				),
				latestTimeline(response.timeline()),
				response.degradation()
		);
	}

	public GovernanceDetailOverviewResponse fromRecommendation(
			GovernanceRecommendationDetailResponse response
	) {
		return new GovernanceDetailOverviewResponse(
				GovernanceDetailType.RECOMMENDATION,
				response.recommendationRecordId(),
				sanitizer.safeText(response.recommendation().title()),
				sanitizer.safeStatus(response.recommendation().status()),
				new GovernanceDetailOverviewCounts(
						1,
						response.approvals().size(),
						response.executionPlans().size(),
						response.verifications().size(),
						0,
						0,
						0
				),
				latestTimeline(response.timeline()),
				response.degradation()
		);
	}

	public GovernanceDetailOverviewResponse fromLearning(
			GovernanceLearningDetailResponse response
	) {
		return new GovernanceDetailOverviewResponse(
				GovernanceDetailType.LEARNING,
				response.learningCandidateId(),
				sanitizer.safeText(response.learningCandidate().title()),
				sanitizer.safeStatus(response.learningCandidate().status()),
				new GovernanceDetailOverviewCounts(
						0,
						response.promotionReviews().size(),
						response.promotionPlans().size(),
						0,
						0,
						1,
						response.knowledgeUpdates().size()
				),
				latestTimeline(response.timeline()),
				response.degradation()
		);
	}

	public GovernanceDetailOverviewResponse fromKnowledgeUpdate(
			GovernanceKnowledgeUpdateDetailResponse response
	) {
		return new GovernanceDetailOverviewResponse(
				GovernanceDetailType.KNOWLEDGE_UPDATE,
				response.knowledgeUpdateApplicationId(),
				sanitizer.safeText(response.knowledgeUpdate().title()),
				sanitizer.safeStatus(response.changeType()),
				new GovernanceDetailOverviewCounts(
						0,
						response.promotionReviews().size(),
						response.promotionPlan() == null ? 0 : 1,
						0,
						0,
						response.learningCandidate() == null ? 0 : 1,
						1
				),
				latestTimeline(response.timeline()),
				response.degradation()
		);
	}

	private GovernanceDetailOverviewTimelineItem latestTimeline(
			List<GovernanceDetailTimelineItem> timeline
	) {
		if (timeline == null || timeline.isEmpty()) {
			return null;
		}

		GovernanceDetailTimelineItem item = timeline.get(timeline.size() - 1);
		return new GovernanceDetailOverviewTimelineItem(
				item.type(),
				item.status(),
				item.occurredAt(),
				sanitizer.safeText(item.summary())
		);
	}
}
