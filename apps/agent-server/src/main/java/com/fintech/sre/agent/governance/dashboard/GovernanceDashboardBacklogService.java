package com.fintech.sre.agent.governance.dashboard;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fintech.sre.agent.incident.lifecycle.IncidentLifecycleRecord;
import com.fintech.sre.agent.incident.lifecycle.IncidentLifecycleStore;
import com.fintech.sre.agent.incident.lifecycle.IncidentStatus;
import com.fintech.sre.agent.learning.application.KnowledgeUpdateApplicationRecord;
import com.fintech.sre.agent.learning.application.KnowledgeUpdateApplicationStore;
import com.fintech.sre.agent.learning.candidate.LearningCandidateRecord;
import com.fintech.sre.agent.learning.candidate.LearningCandidateStatus;
import com.fintech.sre.agent.learning.candidate.LearningCandidateStore;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanRecord;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanStatus;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanStore;
import com.fintech.sre.agent.postmortem.draft.PostmortemDraftRecord;
import com.fintech.sre.agent.postmortem.draft.PostmortemDraftStatus;
import com.fintech.sre.agent.postmortem.draft.PostmortemDraftStore;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalRecord;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalStatus;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalStore;
import com.fintech.sre.agent.recommendation.execution.ExecutionPlanStore;
import com.fintech.sre.agent.recommendation.execution.RecommendationExecutionPlan;
import com.fintech.sre.agent.recommendation.execution.result.HumanExecutionResultRecord;
import com.fintech.sre.agent.recommendation.execution.result.HumanExecutionResultStore;
import com.fintech.sre.agent.recommendation.verification.VerificationResultRecord;
import com.fintech.sre.agent.recommendation.verification.VerificationResultStore;

import reactor.core.publisher.Mono;

@Service
public class GovernanceDashboardBacklogService {

	private final RecommendationApprovalStore approvalStore;
	private final ExecutionPlanStore executionPlanStore;
	private final HumanExecutionResultStore executionResultStore;
	private final VerificationResultStore verificationResultStore;
	private final IncidentLifecycleStore incidentLifecycleStore;
	private final PostmortemDraftStore postmortemDraftStore;
	private final LearningCandidateStore learningCandidateStore;
	private final KnowledgePromotionPlanStore promotionPlanStore;
	private final KnowledgeUpdateApplicationStore knowledgeUpdateStore;

	public GovernanceDashboardBacklogService(
			RecommendationApprovalStore approvalStore,
			ExecutionPlanStore executionPlanStore,
			HumanExecutionResultStore executionResultStore,
			VerificationResultStore verificationResultStore,
			IncidentLifecycleStore incidentLifecycleStore,
			PostmortemDraftStore postmortemDraftStore,
			LearningCandidateStore learningCandidateStore,
			KnowledgePromotionPlanStore promotionPlanStore,
			KnowledgeUpdateApplicationStore knowledgeUpdateStore
	) {
		this.approvalStore = approvalStore;
		this.executionPlanStore = executionPlanStore;
		this.executionResultStore = executionResultStore;
		this.verificationResultStore = verificationResultStore;
		this.incidentLifecycleStore = incidentLifecycleStore;
		this.postmortemDraftStore = postmortemDraftStore;
		this.learningCandidateStore = learningCandidateStore;
		this.promotionPlanStore = promotionPlanStore;
		this.knowledgeUpdateStore = knowledgeUpdateStore;
	}

	public Mono<GovernanceDashboardBacklogSummary> backlog(
			GovernanceDashboardQuery query
	) {
		Instant now = Instant.now();
		GovernanceDashboardTimeRange range =
				(query == null ? new GovernanceDashboardQuery("24h", null, null) : query)
						.toTimeRange(now);

		return Mono.zip(tuple -> buildSummary(
				range,
				filterApprovals(tuple[0], range),
				filterExecutionPlans(tuple[1], range),
				filterExecutionResults(tuple[2], range),
				filterVerifications(tuple[3], range),
				filterIncidentLifecycleRecords(tuple[4], range),
				filterPostmortemDrafts(tuple[5], range),
				filterLearningCandidates(tuple[6], range),
				filterPromotionPlans(tuple[7], range),
				filterKnowledgeUpdates(tuple[8], range)
		),
				approvalStore.findRecent(1000).collectList(),
				executionPlanStore.findRecent(1000).collectList(),
				executionResultStore.findRecent(1000).collectList(),
				verificationResultStore.findRecent(1000).collectList(),
				incidentLifecycleStore.findRecent(2000).collectList(),
				postmortemDraftStore.findRecent(1000).collectList(),
				learningCandidateStore.findRecent(1000).collectList(),
				promotionPlanStore.findRecent(1000).collectList(),
				knowledgeUpdateStore.findRecent(1000).collectList()
		);
	}

	private GovernanceDashboardBacklogSummary buildSummary(
			GovernanceDashboardTimeRange range,
			List<RecommendationApprovalRecord> approvals,
			List<RecommendationExecutionPlan> executionPlans,
			List<HumanExecutionResultRecord> executionResults,
			List<VerificationResultRecord> verifications,
			List<IncidentLifecycleRecord> incidents,
			List<PostmortemDraftRecord> drafts,
			List<LearningCandidateRecord> learningCandidates,
			List<KnowledgePromotionPlanRecord> promotionPlans,
			List<KnowledgeUpdateApplicationRecord> knowledgeUpdates
	) {
		long pendingApprovals = approvals.stream()
				.filter(record -> record.status() == RecommendationApprovalStatus.PENDING)
				.count();

		Set<String> executionPlanRecommendationIds = executionPlans.stream()
				.map(RecommendationExecutionPlan::recommendationRecordId)
				.collect(Collectors.toSet());

		long approvedWithoutExecutionPlan = approvals.stream()
				.filter(record -> record.status() == RecommendationApprovalStatus.APPROVED)
				.map(RecommendationApprovalRecord::recommendationRecordId)
				.filter(id -> !executionPlanRecommendationIds.contains(id))
				.distinct()
				.count();

		Set<String> verifiedExecutionResultIds = verifications.stream()
				.map(VerificationResultRecord::executionResultId)
				.collect(Collectors.toSet());

		long awaitingVerification = executionResults.stream()
				.map(HumanExecutionResultRecord::executionResultId)
				.filter(id -> !verifiedExecutionResultIds.contains(id))
				.distinct()
				.count();

		long unresolvedIncidents = latestIncidentStatuses(incidents).stream()
				.filter(status -> status != null && status != IncidentStatus.RESOLVED)
				.count();

		long draftsAwaitingReview = drafts.stream()
				.filter(record -> record.status() == PostmortemDraftStatus.HUMAN_REVIEW_REQUIRED)
				.count();

		long learningAwaitingReview = learningCandidates.stream()
				.filter(record -> record.status() == LearningCandidateStatus.REVIEW_REQUIRED)
				.count();

		Set<String> appliedPromotionPlanIds = knowledgeUpdates.stream()
				.map(KnowledgeUpdateApplicationRecord::promotionPlanId)
				.collect(Collectors.toSet());

		long promotionAwaitingApplication = promotionPlans.stream()
				.filter(record -> record.status() == KnowledgePromotionPlanStatus.PLAN_CREATED)
				.map(KnowledgePromotionPlanRecord::promotionPlanId)
				.filter(id -> !appliedPromotionPlanIds.contains(id))
				.distinct()
				.count();

		List<GovernanceBacklogItem> items = List.of(
				new GovernanceBacklogItem(
						"pendingRecommendationApprovals",
						pendingApprovals
				),
				new GovernanceBacklogItem(
						"approvedRecommendationsWithoutExecutionPlan",
						approvedWithoutExecutionPlan
				),
				new GovernanceBacklogItem(
						"executionResultsAwaitingVerification",
						awaitingVerification
				),
				new GovernanceBacklogItem(
						"unresolvedIncidents",
						unresolvedIncidents
				),
				new GovernanceBacklogItem(
						"postmortemDraftsAwaitingReview",
						draftsAwaitingReview
				),
				new GovernanceBacklogItem(
						"learningCandidatesAwaitingPromotionReview",
						learningAwaitingReview
				),
				new GovernanceBacklogItem(
						"promotionPlansAwaitingApplication",
						promotionAwaitingApplication
				)
		);

		return new GovernanceDashboardBacklogSummary(
				Instant.now(),
				range,
				items,
				pendingApprovals,
				approvedWithoutExecutionPlan,
				awaitingVerification,
				unresolvedIncidents,
				draftsAwaitingReview,
				learningAwaitingReview,
				promotionAwaitingApplication
		);
	}

	private List<IncidentStatus> latestIncidentStatuses(
			List<IncidentLifecycleRecord> records
	) {
		Map<String, IncidentLifecycleRecord> latestByIncident = records.stream()
				.collect(Collectors.toMap(
						IncidentLifecycleRecord::incidentId,
						Function.identity(),
						(left, right) -> left.transitionedAt().isAfter(right.transitionedAt())
								? left
								: right
				));

		return latestByIncident.values().stream()
				.map(IncidentLifecycleRecord::currentStatus)
				.toList();
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

	@SuppressWarnings("unchecked")
	private List<RecommendationApprovalRecord> filterApprovals(
			Object value,
			GovernanceDashboardTimeRange range
	) {
		return filter(
				(List<RecommendationApprovalRecord>) value,
				range,
				RecommendationApprovalRecord::decidedAt
		);
	}

	@SuppressWarnings("unchecked")
	private List<RecommendationExecutionPlan> filterExecutionPlans(
			Object value,
			GovernanceDashboardTimeRange range
	) {
		return filter(
				(List<RecommendationExecutionPlan>) value,
				range,
				RecommendationExecutionPlan::createdAt
		);
	}

	@SuppressWarnings("unchecked")
	private List<HumanExecutionResultRecord> filterExecutionResults(
			Object value,
			GovernanceDashboardTimeRange range
	) {
		return filter(
				(List<HumanExecutionResultRecord>) value,
				range,
				HumanExecutionResultRecord::recordedAt
		);
	}

	@SuppressWarnings("unchecked")
	private List<VerificationResultRecord> filterVerifications(
			Object value,
			GovernanceDashboardTimeRange range
	) {
		return filter(
				(List<VerificationResultRecord>) value,
				range,
				VerificationResultRecord::verifiedAt
		);
	}

	@SuppressWarnings("unchecked")
	private List<IncidentLifecycleRecord> filterIncidentLifecycleRecords(
			Object value,
			GovernanceDashboardTimeRange range
	) {
		return filter(
				(List<IncidentLifecycleRecord>) value,
				range,
				IncidentLifecycleRecord::transitionedAt
		);
	}

	@SuppressWarnings("unchecked")
	private List<PostmortemDraftRecord> filterPostmortemDrafts(
			Object value,
			GovernanceDashboardTimeRange range
	) {
		return filter(
				(List<PostmortemDraftRecord>) value,
				range,
				PostmortemDraftRecord::createdAt
		);
	}

	@SuppressWarnings("unchecked")
	private List<LearningCandidateRecord> filterLearningCandidates(
			Object value,
			GovernanceDashboardTimeRange range
	) {
		return filter(
				(List<LearningCandidateRecord>) value,
				range,
				LearningCandidateRecord::createdAt
		);
	}

	@SuppressWarnings("unchecked")
	private List<KnowledgePromotionPlanRecord> filterPromotionPlans(
			Object value,
			GovernanceDashboardTimeRange range
	) {
		return filter(
				(List<KnowledgePromotionPlanRecord>) value,
				range,
				KnowledgePromotionPlanRecord::createdAt
		);
	}

	@SuppressWarnings("unchecked")
	private List<KnowledgeUpdateApplicationRecord> filterKnowledgeUpdates(
			Object value,
			GovernanceDashboardTimeRange range
	) {
		return filter(
				(List<KnowledgeUpdateApplicationRecord>) value,
				range,
				KnowledgeUpdateApplicationRecord::appliedAt
		);
	}
}
