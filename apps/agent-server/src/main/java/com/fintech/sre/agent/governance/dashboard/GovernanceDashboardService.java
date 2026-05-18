package com.fintech.sre.agent.governance.dashboard;

import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fintech.sre.agent.incident.lifecycle.IncidentLifecycleRecord;
import com.fintech.sre.agent.incident.lifecycle.IncidentLifecycleStore;
import com.fintech.sre.agent.governance.query.GovernanceDashboardQueryRepository;
import com.fintech.sre.agent.governance.query.GovernanceDashboardQueryResult;
import com.fintech.sre.agent.governance.query.GovernanceQueryMetricsRecorder;
import com.fintech.sre.agent.governance.query.GovernanceQueryResilienceProperties;
import com.fintech.sre.agent.learning.application.KnowledgeUpdateApplicationStore;
import com.fintech.sre.agent.learning.candidate.LearningCandidateStore;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanStore;
import com.fintech.sre.agent.learning.promotion.KnowledgePromotionReviewStore;
import com.fintech.sre.agent.postmortem.draft.PostmortemDraftStore;
import com.fintech.sre.agent.postmortem.review.PostmortemReviewStore;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalStore;
import com.fintech.sre.agent.recommendation.execution.ExecutionPlanStore;
import com.fintech.sre.agent.recommendation.execution.result.HumanExecutionResultStore;
import com.fintech.sre.agent.recommendation.persistence.RecommendationRecord;
import com.fintech.sre.agent.recommendation.persistence.RecommendationRecordStore;
import com.fintech.sre.agent.recommendation.verification.VerificationResultStore;

import reactor.core.publisher.Mono;

@Service
public class GovernanceDashboardService {

	private final RecommendationRecordStore recommendationStore;
	private final RecommendationApprovalStore approvalStore;
	private final ExecutionPlanStore executionPlanStore;
	private final HumanExecutionResultStore executionResultStore;
	private final VerificationResultStore verificationResultStore;
	private final IncidentLifecycleStore lifecycleStore;
	private final PostmortemDraftStore postmortemDraftStore;
	private final PostmortemReviewStore postmortemReviewStore;
	private final LearningCandidateStore learningCandidateStore;
	private final KnowledgePromotionReviewStore promotionReviewStore;
	private final KnowledgePromotionPlanStore promotionPlanStore;
	private final KnowledgeUpdateApplicationStore knowledgeUpdateStore;
	private final GovernanceDashboardQueryRepository queryRepository;
	private final GovernanceQueryMetricsRecorder queryMetricsRecorder;
	private final GovernanceQueryResilienceProperties resilienceProperties;
	private final GovernanceDashboardMetricsRecorder dashboardMetricsRecorder;

	@Autowired
	public GovernanceDashboardService(
			RecommendationRecordStore recommendationStore,
			RecommendationApprovalStore approvalStore,
			ExecutionPlanStore executionPlanStore,
			HumanExecutionResultStore executionResultStore,
			VerificationResultStore verificationResultStore,
			IncidentLifecycleStore lifecycleStore,
			PostmortemDraftStore postmortemDraftStore,
			PostmortemReviewStore postmortemReviewStore,
			LearningCandidateStore learningCandidateStore,
			KnowledgePromotionReviewStore promotionReviewStore,
			KnowledgePromotionPlanStore promotionPlanStore,
			KnowledgeUpdateApplicationStore knowledgeUpdateStore,
			GovernanceQueryMetricsRecorder queryMetricsRecorder,
			GovernanceQueryResilienceProperties resilienceProperties,
			GovernanceDashboardMetricsRecorder dashboardMetricsRecorder
	) {
		this(
				recommendationStore,
				approvalStore,
				executionPlanStore,
				executionResultStore,
				verificationResultStore,
				lifecycleStore,
				postmortemDraftStore,
				postmortemReviewStore,
				learningCandidateStore,
				promotionReviewStore,
				promotionPlanStore,
				knowledgeUpdateStore,
				null,
				queryMetricsRecorder,
				resilienceProperties,
				dashboardMetricsRecorder
		);
	}

	public GovernanceDashboardService(
			RecommendationRecordStore recommendationStore,
			RecommendationApprovalStore approvalStore,
			ExecutionPlanStore executionPlanStore,
			HumanExecutionResultStore executionResultStore,
			VerificationResultStore verificationResultStore,
			IncidentLifecycleStore lifecycleStore,
			PostmortemDraftStore postmortemDraftStore,
			PostmortemReviewStore postmortemReviewStore,
			LearningCandidateStore learningCandidateStore,
			KnowledgePromotionReviewStore promotionReviewStore,
			KnowledgePromotionPlanStore promotionPlanStore,
			KnowledgeUpdateApplicationStore knowledgeUpdateStore,
			GovernanceDashboardQueryRepository queryRepository,
			GovernanceQueryMetricsRecorder queryMetricsRecorder,
			GovernanceQueryResilienceProperties resilienceProperties,
			GovernanceDashboardMetricsRecorder dashboardMetricsRecorder
	) {
		this.recommendationStore = recommendationStore;
		this.approvalStore = approvalStore;
		this.executionPlanStore = executionPlanStore;
		this.executionResultStore = executionResultStore;
		this.verificationResultStore = verificationResultStore;
		this.lifecycleStore = lifecycleStore;
		this.postmortemDraftStore = postmortemDraftStore;
		this.postmortemReviewStore = postmortemReviewStore;
		this.learningCandidateStore = learningCandidateStore;
		this.promotionReviewStore = promotionReviewStore;
		this.promotionPlanStore = promotionPlanStore;
		this.knowledgeUpdateStore = knowledgeUpdateStore;
		this.queryRepository = queryRepository;
		this.queryMetricsRecorder = queryMetricsRecorder;
		this.resilienceProperties = resilienceProperties;
		this.dashboardMetricsRecorder = dashboardMetricsRecorder;
	}

	public Mono<GovernanceDashboardSummary> summary() {
		return summary(new GovernanceDashboardQuery("24h", null, null));
	}

	public Mono<GovernanceDashboardSummary> summary(GovernanceDashboardQuery query) {
		Instant now = Instant.now();
		GovernanceDashboardTimeRange range =
				(query == null ? new GovernanceDashboardQuery("24h", null, null) : query)
						.toTimeRange(now);

		Mono<QueryOutcome<GovernanceDashboardStatusBreakdown>> approvalSummary =
				approvalBreakdown(range);
		Mono<QueryOutcome<GovernanceDashboardStatusBreakdown>> verificationSummary =
				verificationBreakdown(range);
		Mono<QueryOutcome<GovernanceDashboardStatusBreakdown>> incidentSummary =
				incidentBreakdown(range);

		return Mono.zip(objects -> build(
				range,
				filterRecommendationRecords(objects[0], range),
				(QueryOutcome<GovernanceDashboardStatusBreakdown>) objects[1],
				filterExecutionPlans(objects[2], range),
				filterHumanExecutionResults(objects[3], range),
				(QueryOutcome<GovernanceDashboardStatusBreakdown>) objects[4],
				(QueryOutcome<GovernanceDashboardStatusBreakdown>) objects[5],
				filterPostmortemDrafts(objects[6], range),
				filterPostmortemReviews(objects[7], range),
				filterLearningCandidates(objects[8], range),
				filterPromotionReviews(objects[9], range),
				filterPromotionPlans(objects[10], range),
				filterKnowledgeUpdates(objects[11], range)
		),
				recommendationStore.findRecent(1000).collectList(),
				approvalSummary,
				executionPlanStore.findRecent(1000).collectList(),
				executionResultStore.findRecent(1000).collectList(),
				verificationSummary,
				incidentSummary,
				postmortemDraftStore.findRecent(1000).collectList(),
				postmortemReviewStore.findRecent(1000).collectList(),
				learningCandidateStore.findRecent(1000).collectList(),
				promotionReviewStore.findRecent(1000).collectList(),
				promotionPlanStore.findRecent(1000).collectList(),
				knowledgeUpdateStore.findRecent(1000).collectList()
		);
	}

	private GovernanceDashboardSummary build(
			GovernanceDashboardTimeRange range,
			List<RecommendationRecord> recommendations,
			QueryOutcome<GovernanceDashboardStatusBreakdown> approvals,
			List<?> executionPlans,
			List<?> executionResults,
			QueryOutcome<GovernanceDashboardStatusBreakdown> verifications,
			QueryOutcome<GovernanceDashboardStatusBreakdown> incidents,
			List<?> postmortemDrafts,
			List<?> postmortemReviews,
			List<?> learningCandidates,
			List<?> promotionReviews,
			List<?> promotionPlans,
			List<?> knowledgeUpdates
	) {
		GovernanceDashboardSummary summary = new GovernanceDashboardSummary(
				Instant.now(),
				range,
				combineDegradations(
						approvals.degradation(),
						verifications.degradation(),
						incidents.degradation()
				),
				recommendations.size(),
				countBy(recommendations, RecommendationRecord::policyDecision),
				approvals.value(),
				breakdown(executionPlans, this::statusName),
				breakdown(executionResults, this::statusName),
				verifications.value(),
				incidents.value(),
				breakdown(postmortemDrafts, this::statusName),
				breakdown(postmortemReviews, this::statusName),
				breakdown(learningCandidates, this::statusName),
				breakdown(promotionReviews, this::statusName),
				breakdown(promotionPlans, this::statusName),
				knowledgeUpdates.size()
		);
		dashboardMetricsRecorder.recordDegradation("summary", summary.degradation());
		return summary;
	}

	private Mono<QueryOutcome<GovernanceDashboardStatusBreakdown>> approvalBreakdown(
			GovernanceDashboardTimeRange range
	) {
		if (queryRepository != null) {
			Mono<GovernanceDashboardStatusBreakdown> optimized = queryRepository.findApprovalStatusSummary(range)
					.collectList()
					.map(this::breakdownFromResults);
			return optimizedWithPolicy(
					optimized,
					approvalFallback(range),
					"summary",
					"approvalStatus"
			);
		}

		queryMetricsRecorder.fallback("summary", "approvalStatus", "repository_missing");
		return approvalFallback(range)
				.map(value -> new QueryOutcome<>(value, GovernanceDashboardDegradation.none()));
	}

	private Mono<GovernanceDashboardStatusBreakdown> approvalFallback(
			GovernanceDashboardTimeRange range
	) {
		return approvalStore.findRecent(1000)
				.collectList()
				.map(records -> breakdown(
						filter(records, range,
								com.fintech.sre.agent.recommendation.approval.RecommendationApprovalRecord::decidedAt),
						this::statusName
				));
	}

	private Mono<QueryOutcome<GovernanceDashboardStatusBreakdown>> verificationBreakdown(
			GovernanceDashboardTimeRange range
	) {
		if (queryRepository != null) {
			Mono<GovernanceDashboardStatusBreakdown> optimized =
					queryRepository.findVerificationStatusSummary(range)
							.collectList()
							.map(this::breakdownFromResults);
			return optimizedWithPolicy(
					optimized,
					verificationFallback(range),
					"summary",
					"verificationStatus"
			);
		}

		queryMetricsRecorder.fallback("summary", "verificationStatus", "repository_missing");
		return verificationFallback(range)
				.map(value -> new QueryOutcome<>(value, GovernanceDashboardDegradation.none()));
	}

	private Mono<GovernanceDashboardStatusBreakdown> verificationFallback(
			GovernanceDashboardTimeRange range
	) {
		return verificationResultStore.findRecent(1000)
				.collectList()
				.map(records -> breakdown(
						filter(records, range,
								com.fintech.sre.agent.recommendation.verification.VerificationResultRecord::verifiedAt),
						this::statusName
				));
	}

	private Mono<QueryOutcome<GovernanceDashboardStatusBreakdown>> incidentBreakdown(
			GovernanceDashboardTimeRange range
	) {
		if (queryRepository != null) {
			Mono<GovernanceDashboardStatusBreakdown> optimized =
					queryRepository.findLatestIncidentStatusSummary(range)
							.collectList()
							.map(this::breakdownFromResults);
			return optimizedWithPolicy(
					optimized,
					incidentFallback(range),
					"summary",
					"incidentLatestStatus"
			);
		}

		queryMetricsRecorder.fallback("summary", "incidentLatestStatus", "repository_missing");
		return incidentFallback(range)
				.map(value -> new QueryOutcome<>(value, GovernanceDashboardDegradation.none()));
	}

	private Mono<GovernanceDashboardStatusBreakdown> incidentFallback(
			GovernanceDashboardTimeRange range
	) {
		return lifecycleStore.findRecent(2000)
				.collectList()
				.map(records -> incidentLatestStatusBreakdown(
						filter(records, range, IncidentLifecycleRecord::transitionedAt)
				));
	}

	private GovernanceDashboardStatusBreakdown breakdownFromResults(
			List<GovernanceDashboardQueryResult> results
	) {
		if (results == null || results.isEmpty()) {
			return new GovernanceDashboardStatusBreakdown(0, Map.of());
		}

		Map<String, Long> byStatus = results.stream()
				.collect(Collectors.toMap(
						result -> normalize(result.name()),
						GovernanceDashboardQueryResult::count,
						Long::sum,
						java.util.LinkedHashMap::new
				));

		long total = results.stream()
				.mapToLong(GovernanceDashboardQueryResult::count)
				.sum();

		return new GovernanceDashboardStatusBreakdown(total, byStatus);
	}

	private <T> Mono<QueryOutcome<T>> optimizedWithPolicy(
			Mono<T> optimized,
			Mono<T> fallback,
			String queryType,
			String series
	) {
		Mono<T> optimizedMono = optimized;
		if (resilienceProperties.isEnabled()) {
			optimizedMono = optimizedMono.timeout(
					Duration.ofMillis(resilienceProperties.getOptimizedQueryTimeoutMs())
			);
		}

		return optimizedMono
				.doOnSubscribe(ignored ->
						queryMetricsRecorder.optimized(queryType, series))
				.map(value -> new QueryOutcome<>(value, GovernanceDashboardDegradation.none()))
				.onErrorResume(ex -> handleQueryFailure(ex, fallback, queryType, series));
	}

	private <T> Mono<QueryOutcome<T>> handleQueryFailure(
			Throwable ex,
			Mono<T> fallback,
			String queryType,
			String series
	) {
		String reason = ex instanceof java.util.concurrent.TimeoutException
				? "query_timeout"
				: "query_failed";
		queryMetricsRecorder.failure(queryType, series, reason);

		if (!resilienceProperties.isFallbackEnabled()) {
			return Mono.error(ex);
		}

		queryMetricsRecorder.fallback(queryType, series, reason);
		if (!resilienceProperties.isFailOpenDashboard()) {
			return Mono.error(ex);
		}

		return fallback.map(value ->
				new QueryOutcome<>(value, GovernanceDashboardDegradation.fallback(reason)));
	}

	private GovernanceDashboardDegradation combineDegradations(
			GovernanceDashboardDegradation... degradations
	) {
		for (GovernanceDashboardDegradation degradation : degradations) {
			if (degradation != null && degradation.degraded()) {
				return degradation;
			}
		}

		return GovernanceDashboardDegradation.none();
	}

	private GovernanceDashboardStatusBreakdown incidentLatestStatusBreakdown(
			List<IncidentLifecycleRecord> records
	) {
		Map<String, IncidentLifecycleRecord> latestByIncident =
				records.stream()
						.collect(Collectors.toMap(
								IncidentLifecycleRecord::incidentId,
								Function.identity(),
								(left, right) -> left.transitionedAt()
										.isAfter(right.transitionedAt()) ? left : right
						));

		Map<String, Long> byStatus =
				latestByIncident.values().stream()
						.collect(Collectors.groupingBy(
								record -> record.currentStatus() == null
										? "UNKNOWN"
										: record.currentStatus().name(),
								Collectors.counting()
						));

		return new GovernanceDashboardStatusBreakdown(
				latestByIncident.size(),
				byStatus
		);
	}

	private <T> GovernanceDashboardStatusBreakdown breakdown(
			List<T> records,
			Function<T, String> statusExtractor
	) {
		Map<String, Long> byStatus = records.stream()
				.collect(Collectors.groupingBy(
						record -> normalize(statusExtractor.apply(record)),
						Collectors.counting()
				));

		return new GovernanceDashboardStatusBreakdown(records.size(), byStatus);
	}

	private <T> Map<String, Long> countBy(
			List<T> records,
			Function<T, String> extractor
	) {
		return records.stream()
				.collect(Collectors.groupingBy(
						record -> normalize(extractor.apply(record)),
						Collectors.counting()
				));
	}

	private String statusName(Object record) {
		try {
			Object status = record.getClass()
					.getMethod("status")
					.invoke(record);

			return status == null ? "UNKNOWN" : status.toString();
		} catch (Exception ignored) {
			return "UNKNOWN";
		}
	}

	private String normalize(String value) {
		return value == null || value.isBlank() ? "UNKNOWN" : value;
	}

	private <T> List<T> filter(
			List<T> records,
			GovernanceDashboardTimeRange range,
			Function<T, Instant> timeExtractor
	) {
		if (records == null || records.isEmpty()) {
			return List.of();
		}

		return records.stream()
				.filter(record -> range.contains(timeExtractor.apply(record)))
				.toList();
	}

	@SuppressWarnings("unchecked")
	private List<RecommendationRecord> filterRecommendationRecords(Object value, GovernanceDashboardTimeRange range) {
		return filter((List<RecommendationRecord>) value, range, RecommendationRecord::generatedAt);
	}

	@SuppressWarnings("unchecked")
	private List<com.fintech.sre.agent.recommendation.approval.RecommendationApprovalRecord> filterRecommendationApprovalRecords(
			Object value,
			GovernanceDashboardTimeRange range
	) {
		return filter(
				(List<com.fintech.sre.agent.recommendation.approval.RecommendationApprovalRecord>) value,
				range,
				com.fintech.sre.agent.recommendation.approval.RecommendationApprovalRecord::decidedAt
		);
	}

	@SuppressWarnings("unchecked")
	private List<com.fintech.sre.agent.recommendation.execution.RecommendationExecutionPlan> filterExecutionPlans(
			Object value,
			GovernanceDashboardTimeRange range
	) {
		return filter(
				(List<com.fintech.sre.agent.recommendation.execution.RecommendationExecutionPlan>) value,
				range,
				com.fintech.sre.agent.recommendation.execution.RecommendationExecutionPlan::createdAt
		);
	}

	@SuppressWarnings("unchecked")
	private List<com.fintech.sre.agent.recommendation.execution.result.HumanExecutionResultRecord> filterHumanExecutionResults(
			Object value,
			GovernanceDashboardTimeRange range
	) {
		return filter(
				(List<com.fintech.sre.agent.recommendation.execution.result.HumanExecutionResultRecord>) value,
				range,
				com.fintech.sre.agent.recommendation.execution.result.HumanExecutionResultRecord::recordedAt
		);
	}

	@SuppressWarnings("unchecked")
	private List<com.fintech.sre.agent.recommendation.verification.VerificationResultRecord> filterVerificationResults(
			Object value,
			GovernanceDashboardTimeRange range
	) {
		return filter(
				(List<com.fintech.sre.agent.recommendation.verification.VerificationResultRecord>) value,
				range,
				com.fintech.sre.agent.recommendation.verification.VerificationResultRecord::verifiedAt
		);
	}

	@SuppressWarnings("unchecked")
	private List<IncidentLifecycleRecord> filterIncidentLifecycleRecords(
			Object value,
			GovernanceDashboardTimeRange range
	) {
		return filter((List<IncidentLifecycleRecord>) value, range, IncidentLifecycleRecord::transitionedAt);
	}

	@SuppressWarnings("unchecked")
	private List<com.fintech.sre.agent.postmortem.draft.PostmortemDraftRecord> filterPostmortemDrafts(
			Object value,
			GovernanceDashboardTimeRange range
	) {
		return filter(
				(List<com.fintech.sre.agent.postmortem.draft.PostmortemDraftRecord>) value,
				range,
				com.fintech.sre.agent.postmortem.draft.PostmortemDraftRecord::createdAt
		);
	}

	@SuppressWarnings("unchecked")
	private List<com.fintech.sre.agent.postmortem.review.PostmortemReviewRecord> filterPostmortemReviews(
			Object value,
			GovernanceDashboardTimeRange range
	) {
		return filter(
				(List<com.fintech.sre.agent.postmortem.review.PostmortemReviewRecord>) value,
				range,
				com.fintech.sre.agent.postmortem.review.PostmortemReviewRecord::reviewedAt
		);
	}

	@SuppressWarnings("unchecked")
	private List<com.fintech.sre.agent.learning.candidate.LearningCandidateRecord> filterLearningCandidates(
			Object value,
			GovernanceDashboardTimeRange range
	) {
		return filter(
				(List<com.fintech.sre.agent.learning.candidate.LearningCandidateRecord>) value,
				range,
				com.fintech.sre.agent.learning.candidate.LearningCandidateRecord::createdAt
		);
	}

	@SuppressWarnings("unchecked")
	private List<com.fintech.sre.agent.learning.promotion.KnowledgePromotionReviewRecord> filterPromotionReviews(
			Object value,
			GovernanceDashboardTimeRange range
	) {
		return filter(
				(List<com.fintech.sre.agent.learning.promotion.KnowledgePromotionReviewRecord>) value,
				range,
				com.fintech.sre.agent.learning.promotion.KnowledgePromotionReviewRecord::reviewedAt
		);
	}

	@SuppressWarnings("unchecked")
	private List<com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanRecord> filterPromotionPlans(
			Object value,
			GovernanceDashboardTimeRange range
	) {
		return filter(
				(List<com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanRecord>) value,
				range,
				com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanRecord::createdAt
		);
	}

	@SuppressWarnings("unchecked")
	private List<com.fintech.sre.agent.learning.application.KnowledgeUpdateApplicationRecord> filterKnowledgeUpdates(
			Object value,
			GovernanceDashboardTimeRange range
	) {
		return filter(
				(List<com.fintech.sre.agent.learning.application.KnowledgeUpdateApplicationRecord>) value,
				range,
				com.fintech.sre.agent.learning.application.KnowledgeUpdateApplicationRecord::appliedAt
		);
	}

	private record QueryOutcome<T>(
			T value,
			GovernanceDashboardDegradation degradation
	) {
	}
}
