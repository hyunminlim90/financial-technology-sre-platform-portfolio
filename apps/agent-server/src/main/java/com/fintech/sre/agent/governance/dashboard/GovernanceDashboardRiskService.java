package com.fintech.sre.agent.governance.dashboard;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import org.springframework.stereotype.Service;

import com.fintech.sre.agent.incident.lifecycle.IncidentLifecycleRecord;
import com.fintech.sre.agent.incident.lifecycle.IncidentLifecycleStore;
import com.fintech.sre.agent.incident.lifecycle.IncidentStatus;
import com.fintech.sre.agent.learning.candidate.LearningCandidateRecord;
import com.fintech.sre.agent.learning.candidate.LearningCandidateStatus;
import com.fintech.sre.agent.learning.candidate.LearningCandidateStore;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanRecord;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanStatus;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanStore;
import com.fintech.sre.agent.postmortem.review.PostmortemReviewRecord;
import com.fintech.sre.agent.postmortem.review.PostmortemReviewStatus;
import com.fintech.sre.agent.postmortem.review.PostmortemReviewStore;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalRecord;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalStatus;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalStore;
import com.fintech.sre.agent.recommendation.verification.VerificationResultRecord;
import com.fintech.sre.agent.recommendation.verification.VerificationResultStore;
import com.fintech.sre.agent.recommendation.verification.VerificationStatus;

import reactor.core.publisher.Mono;

@Service
public class GovernanceDashboardRiskService {

	private static final double APPROVAL_REJECT_RATE_THRESHOLD = 0.30;
	private static final double VERIFICATION_FAILURE_RATE_THRESHOLD = 0.25;
	private static final double INCIDENT_REOPEN_RATE_THRESHOLD = 0.15;
	private static final long LEARNING_BACKLOG_THRESHOLD = 10;
	private static final long PROMOTION_BACKLOG_THRESHOLD = 5;
	private static final double POSTMORTEM_REVISION_RATE_THRESHOLD = 0.20;

	private final RecommendationApprovalStore approvalStore;
	private final VerificationResultStore verificationStore;
	private final IncidentLifecycleStore incidentLifecycleStore;
	private final LearningCandidateStore learningCandidateStore;
	private final KnowledgePromotionPlanStore promotionPlanStore;
	private final PostmortemReviewStore postmortemReviewStore;

	public GovernanceDashboardRiskService(
			RecommendationApprovalStore approvalStore,
			VerificationResultStore verificationStore,
			IncidentLifecycleStore incidentLifecycleStore,
			LearningCandidateStore learningCandidateStore,
			KnowledgePromotionPlanStore promotionPlanStore,
			PostmortemReviewStore postmortemReviewStore
	) {
		this.approvalStore = approvalStore;
		this.verificationStore = verificationStore;
		this.incidentLifecycleStore = incidentLifecycleStore;
		this.learningCandidateStore = learningCandidateStore;
		this.promotionPlanStore = promotionPlanStore;
		this.postmortemReviewStore = postmortemReviewStore;
	}

	public Mono<GovernanceDashboardRiskSummary> summary(
			GovernanceDashboardQuery query
	) {
		Instant now = Instant.now();
		GovernanceDashboardTimeRange range =
				(query == null ? new GovernanceDashboardQuery("24h", null, null) : query)
						.toTimeRange(now);

		return Mono.zip(
				approvalStore.findRecent(2000).collectList(),
				verificationStore.findRecent(2000).collectList(),
				incidentLifecycleStore.findRecent(3000).collectList(),
				learningCandidateStore.findRecent(2000).collectList(),
				promotionPlanStore.findRecent(2000).collectList(),
				postmortemReviewStore.findRecent(2000).collectList()
		).map(tuple -> {
			List<RecommendationApprovalRecord> approvals =
					filter(tuple.getT1(), range, RecommendationApprovalRecord::decidedAt);
			List<VerificationResultRecord> verifications =
					filter(tuple.getT2(), range, VerificationResultRecord::verifiedAt);
			List<IncidentLifecycleRecord> incidents =
					filter(tuple.getT3(), range, IncidentLifecycleRecord::transitionedAt);
			List<LearningCandidateRecord> learningCandidates =
					filter(tuple.getT4(), range, LearningCandidateRecord::createdAt);
			List<KnowledgePromotionPlanRecord> promotionPlans =
					filter(tuple.getT5(), range, KnowledgePromotionPlanRecord::createdAt);
			List<PostmortemReviewRecord> reviews =
					filter(tuple.getT6(), range, PostmortemReviewRecord::reviewedAt);

			List<GovernanceRiskIndicator> indicators = new ArrayList<>();
			indicators.add(approvalRejectRate(approvals));
			indicators.add(verificationFailureRate(verifications));
			indicators.add(incidentReopenRate(incidents));
			indicators.add(learningBacklog(learningCandidates));
			indicators.add(promotionBacklog(promotionPlans));
			indicators.add(postmortemRevisionRate(reviews));

			GovernanceRiskLevel overall = indicators.stream()
					.map(GovernanceRiskIndicator::level)
					.max(Comparator.naturalOrder())
					.orElse(GovernanceRiskLevel.LOW);

			return new GovernanceDashboardRiskSummary(
					Instant.now(),
					range,
					overall,
					indicators
			);
		});
	}

	private GovernanceRiskIndicator approvalRejectRate(
			List<RecommendationApprovalRecord> approvals
	) {
		long total = approvals.size();
		long rejected = approvals.stream()
				.filter(record -> record.status() == RecommendationApprovalStatus.REJECTED)
				.count();
		double rate = ratio(rejected, total);
		return indicator(
				"approvalRejectRate",
				rate,
				APPROVAL_REJECT_RATE_THRESHOLD,
				"Rejected recommendation approvals indicate governance disagreement or low recommendation quality."
		);
	}

	private GovernanceRiskIndicator verificationFailureRate(
			List<VerificationResultRecord> verifications
	) {
		long total = verifications.size();
		long failed = verifications.stream()
				.filter(record -> record.status() == VerificationStatus.NOT_VERIFIED
						|| record.status() == VerificationStatus.REGRESSION_DETECTED)
				.count();
		double rate = ratio(failed, total);
		return indicator(
				"verificationFailureRate",
				rate,
				VERIFICATION_FAILURE_RATE_THRESHOLD,
				"Verification failures or regressions indicate unstable mitigation or operational recovery."
		);
	}

	private GovernanceRiskIndicator incidentReopenRate(
			List<IncidentLifecycleRecord> incidents
	) {
		long total = incidents.size();
		long reopened = incidents.stream()
				.filter(record -> record.currentStatus() == IncidentStatus.REOPENED
						|| record.currentStatus() == IncidentStatus.ESCALATED)
				.count();
		double rate = ratio(reopened, total);
		return indicator(
				"incidentReopenRate",
				rate,
				INCIDENT_REOPEN_RATE_THRESHOLD,
				"Reopened or escalated incidents indicate unstable incident resolution."
		);
	}

	private GovernanceRiskIndicator learningBacklog(
			List<LearningCandidateRecord> records
	) {
		long backlog = records.stream()
				.filter(record -> record.status() == LearningCandidateStatus.REVIEW_REQUIRED)
				.count();
		return absoluteIndicator(
				"learningBacklog",
				backlog,
				LEARNING_BACKLOG_THRESHOLD,
				"Learning candidate backlog indicates delayed operational learning review."
		);
	}

	private GovernanceRiskIndicator promotionBacklog(
			List<KnowledgePromotionPlanRecord> records
	) {
		long backlog = records.stream()
				.filter(record -> record.status() == KnowledgePromotionPlanStatus.PLAN_CREATED)
				.count();
		return absoluteIndicator(
				"promotionPlanBacklog",
				backlog,
				PROMOTION_BACKLOG_THRESHOLD,
				"Knowledge promotion plans are waiting for human application."
		);
	}

	private GovernanceRiskIndicator postmortemRevisionRate(
			List<PostmortemReviewRecord> reviews
	) {
		long total = reviews.size();
		long revisions = reviews.stream()
				.filter(record -> record.status() == PostmortemReviewStatus.NEEDS_REVISION)
				.count();
		double rate = ratio(revisions, total);
		return indicator(
				"postmortemRevisionRate",
				rate,
				POSTMORTEM_REVISION_RATE_THRESHOLD,
				"Postmortem revision rate indicates unstable incident learning quality."
		);
	}

	private GovernanceRiskIndicator indicator(
			String name,
			double value,
			double threshold,
			String reason
	) {
		GovernanceRiskLevel level;
		if (value >= threshold * 2) {
			level = GovernanceRiskLevel.CRITICAL;
		} else if (value >= threshold * 1.5) {
			level = GovernanceRiskLevel.HIGH;
		} else if (value >= threshold) {
			level = GovernanceRiskLevel.MEDIUM;
		} else {
			level = GovernanceRiskLevel.LOW;
		}

		return new GovernanceRiskIndicator(name, level, value, threshold, reason);
	}

	private GovernanceRiskIndicator absoluteIndicator(
			String name,
			long value,
			long threshold,
			String reason
	) {
		GovernanceRiskLevel level;
		if (value >= threshold * 3) {
			level = GovernanceRiskLevel.CRITICAL;
		} else if (value >= threshold * 2) {
			level = GovernanceRiskLevel.HIGH;
		} else if (value >= threshold) {
			level = GovernanceRiskLevel.MEDIUM;
		} else {
			level = GovernanceRiskLevel.LOW;
		}

		return new GovernanceRiskIndicator(name, level, value, threshold, reason);
	}

	private double ratio(long numerator, long denominator) {
		if (denominator <= 0) {
			return 0D;
		}
		return (double) numerator / denominator;
	}

	private <T> List<T> filter(
			List<T> records,
			GovernanceDashboardTimeRange range,
			Function<T, Instant> extractor
	) {
		if (records == null || records.isEmpty()) {
			return List.of();
		}

		return records.stream()
				.filter(record -> range.contains(extractor.apply(record)))
				.toList();
	}
}
