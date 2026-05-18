package com.fintech.sre.agent.governance.dashboard;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fintech.sre.agent.governance.query.GovernanceDashboardQueryRepository;
import com.fintech.sre.agent.governance.query.GovernanceDashboardTimeBucketResult;
import com.fintech.sre.agent.governance.query.GovernanceQueryMetricsRecorder;
import com.fintech.sre.agent.governance.query.GovernanceQueryResilienceProperties;
import com.fintech.sre.agent.incident.lifecycle.IncidentLifecycleRecord;
import com.fintech.sre.agent.incident.lifecycle.IncidentLifecycleStore;
import com.fintech.sre.agent.learning.application.KnowledgeUpdateApplicationRecord;
import com.fintech.sre.agent.learning.application.KnowledgeUpdateApplicationStore;
import com.fintech.sre.agent.learning.candidate.LearningCandidateRecord;
import com.fintech.sre.agent.learning.candidate.LearningCandidateStore;
import com.fintech.sre.agent.postmortem.review.PostmortemReviewRecord;
import com.fintech.sre.agent.postmortem.review.PostmortemReviewStore;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalRecord;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalStore;
import com.fintech.sre.agent.recommendation.persistence.RecommendationRecord;
import com.fintech.sre.agent.recommendation.persistence.RecommendationRecordStore;
import com.fintech.sre.agent.recommendation.verification.VerificationResultRecord;
import com.fintech.sre.agent.recommendation.verification.VerificationResultStore;

import reactor.core.publisher.Mono;

@Service
public class GovernanceDashboardTrendService {

	private final RecommendationRecordStore recommendationStore;
	private final RecommendationApprovalStore approvalStore;
	private final VerificationResultStore verificationStore;
	private final IncidentLifecycleStore incidentLifecycleStore;
	private final PostmortemReviewStore postmortemReviewStore;
	private final LearningCandidateStore learningCandidateStore;
	private final KnowledgeUpdateApplicationStore knowledgeUpdateStore;
	private final GovernanceDashboardQueryRepository queryRepository;
	private final GovernanceQueryMetricsRecorder queryMetricsRecorder;
	private final GovernanceQueryResilienceProperties resilienceProperties;
	private final GovernanceDashboardMetricsRecorder dashboardMetricsRecorder;

	@Autowired
	public GovernanceDashboardTrendService(
			RecommendationRecordStore recommendationStore,
			RecommendationApprovalStore approvalStore,
			VerificationResultStore verificationStore,
			IncidentLifecycleStore incidentLifecycleStore,
			PostmortemReviewStore postmortemReviewStore,
			LearningCandidateStore learningCandidateStore,
			KnowledgeUpdateApplicationStore knowledgeUpdateStore,
			GovernanceQueryMetricsRecorder queryMetricsRecorder,
			GovernanceQueryResilienceProperties resilienceProperties,
			GovernanceDashboardMetricsRecorder dashboardMetricsRecorder
	) {
		this(
				recommendationStore,
				approvalStore,
				verificationStore,
				incidentLifecycleStore,
				postmortemReviewStore,
				learningCandidateStore,
				knowledgeUpdateStore,
				null,
				queryMetricsRecorder,
				resilienceProperties,
				dashboardMetricsRecorder
		);
	}

	public GovernanceDashboardTrendService(
			RecommendationRecordStore recommendationStore,
			RecommendationApprovalStore approvalStore,
			VerificationResultStore verificationStore,
			IncidentLifecycleStore incidentLifecycleStore,
			PostmortemReviewStore postmortemReviewStore,
			LearningCandidateStore learningCandidateStore,
			KnowledgeUpdateApplicationStore knowledgeUpdateStore,
			GovernanceDashboardQueryRepository queryRepository,
			GovernanceQueryMetricsRecorder queryMetricsRecorder,
			GovernanceQueryResilienceProperties resilienceProperties,
			GovernanceDashboardMetricsRecorder dashboardMetricsRecorder
	) {
		this.recommendationStore = recommendationStore;
		this.approvalStore = approvalStore;
		this.verificationStore = verificationStore;
		this.incidentLifecycleStore = incidentLifecycleStore;
		this.postmortemReviewStore = postmortemReviewStore;
		this.learningCandidateStore = learningCandidateStore;
		this.knowledgeUpdateStore = knowledgeUpdateStore;
		this.queryRepository = queryRepository;
		this.queryMetricsRecorder = queryMetricsRecorder;
		this.resilienceProperties = resilienceProperties;
		this.dashboardMetricsRecorder = dashboardMetricsRecorder;
	}

	public Mono<GovernanceDashboardTrendSummary> trends(
			GovernanceDashboardTrendQuery query
	) {
		Instant now = Instant.now();
		GovernanceDashboardTrendQuery safeQuery =
				query == null
						? new GovernanceDashboardTrendQuery("24h", null, null, "1h")
						: query;

		GovernanceDashboardTimeRange range = safeQuery.toTimeRange(now);
		GovernanceDashboardBucketSize bucketSize = safeQuery.toBucketSize();
		Duration bucketDuration = bucketSize.duration();

		validateBucket(range, bucketSize);

		Mono<QueryOutcome<GovernanceTrendSeries>> approvalSeries =
				approvalSeries(range, bucketSize, bucketDuration);
		Mono<QueryOutcome<GovernanceTrendSeries>> verificationSeries =
				verificationSeries(range, bucketSize, bucketDuration);
		Mono<QueryOutcome<GovernanceTrendSeries>> incidentSeries =
				incidentSeries(range, bucketSize, bucketDuration);

		return Mono.zip(
				recommendationStore.findRecent(2000).collectList(),
				approvalSeries,
				verificationSeries,
				incidentSeries,
				postmortemReviewStore.findRecent(2000).collectList(),
				learningCandidateStore.findRecent(2000).collectList(),
				knowledgeUpdateStore.findRecent(2000).collectList()
		).map(tuple -> {
			GovernanceDashboardTrendSummary summary = new GovernanceDashboardTrendSummary(
					Instant.now(),
					range,
					combineDegradations(
							((QueryOutcome<GovernanceTrendSeries>) tuple.getT2()).degradation(),
							((QueryOutcome<GovernanceTrendSeries>) tuple.getT3()).degradation(),
							((QueryOutcome<GovernanceTrendSeries>) tuple.getT4()).degradation()
					),
					bucketLabel(bucketSize),
					List.of(
							series(
									"recommendationsCreated",
									tuple.getT1(),
									range,
									bucketDuration,
									RecommendationRecord::generatedAt,
									record -> normalize(record.policyDecision())
							),
							((QueryOutcome<GovernanceTrendSeries>) tuple.getT2()).value(),
							((QueryOutcome<GovernanceTrendSeries>) tuple.getT3()).value(),
							((QueryOutcome<GovernanceTrendSeries>) tuple.getT4()).value(),
							series(
									"postmortemReviews",
									tuple.getT5(),
									range,
									bucketDuration,
									PostmortemReviewRecord::reviewedAt,
									record -> record.status() == null
											? "UNKNOWN"
											: record.status().name()
							),
							series(
									"learningCandidates",
									tuple.getT6(),
									range,
									bucketDuration,
									LearningCandidateRecord::createdAt,
									record -> record.status() == null
											? "UNKNOWN"
											: record.status().name()
							),
							series(
									"knowledgeUpdates",
									tuple.getT7(),
									range,
									bucketDuration,
									KnowledgeUpdateApplicationRecord::appliedAt,
									record -> record.changeType() == null
											? "UNKNOWN"
											: record.changeType().name()
							)
					)
			);
			dashboardMetricsRecorder.recordDegradation("trends", summary.degradation());
			return summary;
		});
	}

	private Mono<QueryOutcome<GovernanceTrendSeries>> approvalSeries(
			GovernanceDashboardTimeRange range,
			GovernanceDashboardBucketSize bucketSize,
			Duration bucketDuration
	) {
		if (queryRepository != null) {
			Mono<GovernanceTrendSeries> optimized =
					queryRepository.findApprovalStatusBuckets(range, bucketSize)
							.collectList()
							.map(results -> seriesFromBucketResults(
									"approvalDecisions",
									results,
									range,
									bucketDuration
							));
			return optimizedWithPolicy(
					optimized,
					approvalFallbackSeries(range, bucketDuration),
					"trend",
					"approvalDecisions"
			);
		}

		queryMetricsRecorder.fallback("trend", "approvalDecisions", "repository_missing");
		return approvalFallbackSeries(range, bucketDuration)
				.map(value -> new QueryOutcome<>(value, GovernanceDashboardDegradation.none()));
	}

	private Mono<GovernanceTrendSeries> approvalFallbackSeries(
			GovernanceDashboardTimeRange range,
			Duration bucketDuration
	) {
		return approvalStore.findRecent(2000)
				.collectList()
				.map(records -> series(
						"approvalDecisions",
						records,
						range,
						bucketDuration,
						RecommendationApprovalRecord::decidedAt,
						record -> record.status() == null
								? "UNKNOWN"
								: record.status().name()
				));
	}

	private Mono<QueryOutcome<GovernanceTrendSeries>> verificationSeries(
			GovernanceDashboardTimeRange range,
			GovernanceDashboardBucketSize bucketSize,
			Duration bucketDuration
	) {
		if (queryRepository != null) {
			Mono<GovernanceTrendSeries> optimized =
					queryRepository.findVerificationStatusBuckets(range, bucketSize)
							.collectList()
							.map(results -> seriesFromBucketResults(
									"verificationResults",
									results,
									range,
									bucketDuration
							));
			return optimizedWithPolicy(
					optimized,
					verificationFallbackSeries(range, bucketDuration),
					"trend",
					"verificationResults"
			);
		}

		queryMetricsRecorder.fallback("trend", "verificationResults", "repository_missing");
		return verificationFallbackSeries(range, bucketDuration)
				.map(value -> new QueryOutcome<>(value, GovernanceDashboardDegradation.none()));
	}

	private Mono<GovernanceTrendSeries> verificationFallbackSeries(
			GovernanceDashboardTimeRange range,
			Duration bucketDuration
	) {
		return verificationStore.findRecent(2000)
				.collectList()
				.map(records -> series(
						"verificationResults",
						records,
						range,
						bucketDuration,
						VerificationResultRecord::verifiedAt,
						record -> record.status() == null
								? "UNKNOWN"
								: record.status().name()
				));
	}

	private Mono<QueryOutcome<GovernanceTrendSeries>> incidentSeries(
			GovernanceDashboardTimeRange range,
			GovernanceDashboardBucketSize bucketSize,
			Duration bucketDuration
	) {
		if (queryRepository != null) {
			Mono<GovernanceTrendSeries> optimized =
					queryRepository.findIncidentLifecycleStatusBuckets(range, bucketSize)
							.collectList()
							.map(results -> seriesFromBucketResults(
									"incidentLifecycleTransitions",
									results,
									range,
									bucketDuration
							));
			return optimizedWithPolicy(
					optimized,
					incidentFallbackSeries(range, bucketDuration),
					"trend",
					"incidentLifecycleTransitions"
			);
		}

		queryMetricsRecorder.fallback("trend", "incidentLifecycleTransitions", "repository_missing");
		return incidentFallbackSeries(range, bucketDuration)
				.map(value -> new QueryOutcome<>(value, GovernanceDashboardDegradation.none()));
	}

	private Mono<GovernanceTrendSeries> incidentFallbackSeries(
			GovernanceDashboardTimeRange range,
			Duration bucketDuration
	) {
		return incidentLifecycleStore.findRecent(3000)
				.collectList()
				.map(records -> series(
						"incidentLifecycleTransitions",
						records,
						range,
						bucketDuration,
						IncidentLifecycleRecord::transitionedAt,
						record -> record.currentStatus() == null
								? "UNKNOWN"
								: record.currentStatus().name()
				));
	}

	private String bucketLabel(GovernanceDashboardBucketSize bucketSize) {
		return switch (bucketSize) {
			case FIFTEEN_MINUTES -> "15m";
			case ONE_HOUR -> "1h";
			case ONE_DAY -> "1d";
		};
	}

	private <T> GovernanceTrendSeries series(
			String name,
			List<T> records,
			GovernanceDashboardTimeRange range,
			Duration bucketDuration,
			Function<T, Instant> timeExtractor,
			Function<T, String> statusExtractor
	) {
		List<Bucket> buckets = buckets(range, bucketDuration);
		List<GovernanceTrendPoint> points = buckets.stream()
				.map(bucket -> point(bucket, records, timeExtractor, statusExtractor))
				.toList();
		return new GovernanceTrendSeries(name, points);
	}

	private GovernanceTrendSeries seriesFromBucketResults(
			String name,
			List<GovernanceDashboardTimeBucketResult> results,
			GovernanceDashboardTimeRange range,
			Duration bucketDuration
	) {
		Map<Instant, Map<String, Long>> grouped = new LinkedHashMap<>();
		if (results != null) {
			for (GovernanceDashboardTimeBucketResult result : results) {
				grouped.computeIfAbsent(result.bucketStart(), ignored -> new LinkedHashMap<>())
						.merge(normalize(result.name()), result.count(), Long::sum);
			}
		}

		List<GovernanceTrendPoint> points = buckets(range, bucketDuration).stream()
				.map(bucket -> {
					Map<String, Long> byStatus = grouped.getOrDefault(
							bucket.start(),
							Map.of()
					);
					long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
					return new GovernanceTrendPoint(
							bucket.start(),
							bucket.end(),
							total,
							byStatus
					);
				})
				.toList();

		return new GovernanceTrendSeries(name, points);
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
				.doOnSubscribe(ignored -> queryMetricsRecorder.optimized(queryType, series))
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

	private <T> GovernanceTrendPoint point(
			Bucket bucket,
			List<T> records,
			Function<T, Instant> timeExtractor,
			Function<T, String> statusExtractor
	) {
		List<T> matched = records == null
				? List.of()
				: records.stream()
				.filter(record -> bucket.contains(timeExtractor.apply(record)))
				.toList();

		Map<String, Long> byStatus = matched.stream()
				.collect(Collectors.groupingBy(
						record -> normalize(statusExtractor.apply(record)),
						Collectors.counting()
				));

		return new GovernanceTrendPoint(
				bucket.start(),
				bucket.end(),
				matched.size(),
				byStatus
		);
	}

	private List<Bucket> buckets(
			GovernanceDashboardTimeRange range,
			Duration bucketDuration
	) {
		List<Bucket> buckets = new ArrayList<>();
		Instant cursor = range.from();

		while (cursor.isBefore(range.to())) {
			Instant end = cursor.plus(bucketDuration);
			if (end.isAfter(range.to())) {
				end = range.to();
			}
			buckets.add(new Bucket(cursor, end));
			cursor = end;
		}

		return buckets;
	}

	private void validateBucket(
			GovernanceDashboardTimeRange range,
			GovernanceDashboardBucketSize bucket
	) {
		long bucketCount = Duration.between(range.from(), range.to())
				.dividedBy(bucket.duration());

		if (bucketCount > 500) {
			throw new GovernanceDashboardRejectedException(
					"DASHBOARD_BUCKET_COUNT_TOO_LARGE",
					"Dashboard trend bucket count must be <= 500."
			);
		}
	}

	private String normalize(String value) {
		return value == null || value.isBlank() ? "UNKNOWN" : value;
	}

	private record Bucket(
			Instant start,
			Instant end
	) {
		boolean contains(Instant instant) {
			if (instant == null) {
				return false;
			}

			return !instant.isBefore(start)
					&& instant.isBefore(end);
		}
	}

	private record QueryOutcome<T>(
			T value,
			GovernanceDashboardDegradation degradation
	) {
	}
}
