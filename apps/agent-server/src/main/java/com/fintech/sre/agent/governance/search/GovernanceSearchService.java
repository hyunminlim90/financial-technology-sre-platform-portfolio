package com.fintech.sre.agent.governance.search;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.fintech.sre.agent.governance.dashboard.GovernanceDashboardQuery;
import com.fintech.sre.agent.governance.dashboard.GovernanceDashboardTimeRange;
import com.fintech.sre.agent.governance.detail.GovernanceDetailSanitizer;
import com.fintech.sre.agent.incident.lifecycle.IncidentLifecycleRecord;
import com.fintech.sre.agent.incident.lifecycle.IncidentLifecycleStore;
import com.fintech.sre.agent.learning.application.KnowledgeUpdateApplicationRecord;
import com.fintech.sre.agent.learning.application.KnowledgeUpdateApplicationStore;
import com.fintech.sre.agent.learning.candidate.LearningCandidateRecord;
import com.fintech.sre.agent.learning.candidate.LearningCandidateStore;
import com.fintech.sre.agent.recommendation.persistence.RecommendationRecord;
import com.fintech.sre.agent.recommendation.persistence.RecommendationRecordStore;

import reactor.core.publisher.Mono;

@Service
public class GovernanceSearchService {

	private final RecommendationRecordStore recommendationStore;
	private final IncidentLifecycleStore incidentLifecycleStore;
	private final LearningCandidateStore learningCandidateStore;
	private final KnowledgeUpdateApplicationStore knowledgeUpdateStore;
	private final GovernanceDetailSanitizer sanitizer;
	private final GovernanceSearchMetricsRecorder metricsRecorder;
	private final GovernanceSearchResilienceProperties resilienceProperties;

	public GovernanceSearchService(
			RecommendationRecordStore recommendationStore,
			IncidentLifecycleStore incidentLifecycleStore,
			LearningCandidateStore learningCandidateStore,
			KnowledgeUpdateApplicationStore knowledgeUpdateStore,
			GovernanceDetailSanitizer sanitizer,
			GovernanceSearchMetricsRecorder metricsRecorder,
			GovernanceSearchResilienceProperties resilienceProperties
	) {
		this.recommendationStore = recommendationStore;
		this.incidentLifecycleStore = incidentLifecycleStore;
		this.learningCandidateStore = learningCandidateStore;
		this.knowledgeUpdateStore = knowledgeUpdateStore;
		this.sanitizer = sanitizer;
		this.metricsRecorder = metricsRecorder;
		this.resilienceProperties = resilienceProperties;
	}

	public Mono<GovernanceSearchResponse> search(GovernanceSearchQuery query) {
		GovernanceSearchQuery safeQuery = query == null
				? new GovernanceSearchQuery("", GovernanceSearchType.ALL, "24h", 20)
				: query;
		GovernanceSearchType type = safeQuery.type() == null
				? GovernanceSearchType.ALL
				: safeQuery.type();
		int limit = safeQuery.safeLimit();
		String normalizedQuery = safeQuery.normalizedQuery();
		GovernanceDashboardTimeRange range = new GovernanceDashboardQuery(
				safeQuery.window(),
				null,
				null
		).toTimeRange(Instant.now());
		int fetchLimit = Math.min(Math.max(limit * 5, 50), 500);

		return (type == GovernanceSearchType.ALL
				? searchAll(normalizedQuery, range, limit, fetchLimit)
				: searchSingleType(type, normalizedQuery, range, limit, fetchLimit))
				.doOnNext(response -> {
					metricsRecorder.success(
							response.type(),
							response.results().size()
					);
					recordDegraded(response);
				})
				.doOnError(ex -> metricsRecorder.failure(type));
	}

	private void recordDegraded(GovernanceSearchResponse response) {
		if (response == null
				|| response.degradation() == null
				|| !response.degradation().degraded()) {
			return;
		}

		response.degradation().failedComponents().forEach(component ->
				metricsRecorder.degraded(
						response.type(),
						response.degradation().reason(),
						component
				)
		);
	}

	private Mono<GovernanceSearchResponse> searchAll(
			String normalizedQuery,
			GovernanceDashboardTimeRange range,
			int limit,
			int fetchLimit
	) {
		List<String> failedComponents = new CopyOnWriteArrayList<>();
		AtomicReference<String> degradationReason = new AtomicReference<>("none");

		return Mono.zip(
				component(
						"recommendation",
						searchRecommendations(normalizedQuery, range, fetchLimit),
						failedComponents,
						degradationReason
				),
				component(
						"incident",
						searchIncidents(normalizedQuery, range, fetchLimit),
						failedComponents,
						degradationReason
				),
				component(
						"learningCandidate",
						searchLearningCandidates(normalizedQuery, range, fetchLimit),
						failedComponents,
						degradationReason
				),
				component(
						"knowledgeUpdate",
						searchKnowledgeUpdates(normalizedQuery, range, fetchLimit),
						failedComponents,
						degradationReason
				)
		).map(tuple -> response(
				normalizedQuery,
				GovernanceSearchType.ALL,
				limit,
				Stream.of(tuple.getT1(), tuple.getT2(), tuple.getT3(), tuple.getT4())
						.flatMap(List::stream)
						.toList(),
				failedComponents,
				degradationReason.get()
		));
	}

	private Mono<GovernanceSearchResponse> searchSingleType(
			GovernanceSearchType type,
			String normalizedQuery,
			GovernanceDashboardTimeRange range,
			int limit,
			int fetchLimit
	) {
		Mono<List<GovernanceSearchResult>> source = switch (type) {
			case INCIDENT -> searchIncidents(normalizedQuery, range, fetchLimit);
			case RECOMMENDATION -> searchRecommendations(normalizedQuery, range, fetchLimit);
			case LEARNING_CANDIDATE -> searchLearningCandidates(normalizedQuery, range, fetchLimit);
			case KNOWLEDGE_UPDATE -> searchKnowledgeUpdates(normalizedQuery, range, fetchLimit);
			case ALL -> Mono.just(List.of());
		};

		return source.map(results -> response(
				normalizedQuery,
				type,
				limit,
				results,
				List.of(),
				"none"
		));
	}

	private boolean shouldLoad(
			GovernanceSearchType requested,
			GovernanceSearchType target
	) {
		return requested == GovernanceSearchType.ALL || requested == target;
	}

	private Mono<List<GovernanceSearchResult>> searchRecommendations(
			String query,
			GovernanceDashboardTimeRange range,
			int fetchLimit
	) {
		return recommendationStore.findRecent(fetchLimit)
				.collectList()
				.map(records -> recommendationResults(records, range, query));
	}

	private Mono<List<GovernanceSearchResult>> searchIncidents(
			String query,
			GovernanceDashboardTimeRange range,
			int fetchLimit
	) {
		return incidentLifecycleStore.findRecent(fetchLimit)
				.collectList()
				.map(records -> incidentResults(records, range, query));
	}

	private Mono<List<GovernanceSearchResult>> searchLearningCandidates(
			String query,
			GovernanceDashboardTimeRange range,
			int fetchLimit
	) {
		return learningCandidateStore.findRecent(fetchLimit)
				.collectList()
				.map(records -> learningResults(records, range, query));
	}

	private Mono<List<GovernanceSearchResult>> searchKnowledgeUpdates(
			String query,
			GovernanceDashboardTimeRange range,
			int fetchLimit
	) {
		return knowledgeUpdateStore.findRecent(fetchLimit)
				.collectList()
				.map(records -> knowledgeUpdateResults(records, range, query));
	}

	private Mono<List<GovernanceSearchResult>> component(
			String component,
			Mono<List<GovernanceSearchResult>> source,
			List<String> failedComponents,
			AtomicReference<String> degradationReason
	) {
		if (!resilienceProperties.isEnabled()) {
			return source;
		}

		return source.timeout(Duration.ofMillis(
						resilienceProperties.getComponentQueryTimeoutMs()
				))
				.onErrorResume(ex -> {
					if (!resilienceProperties.isPartialSearchEnabled()
							|| !resilienceProperties.isFailOpenSearch()) {
						return Mono.error(ex);
					}

					failedComponents.add(component);
					degradationReason.compareAndSet("none", reasonFor(ex));
					return Mono.just(List.of());
				});
	}

	private GovernanceSearchResponse response(
			String normalizedQuery,
			GovernanceSearchType type,
			int limit,
			List<GovernanceSearchResult> results,
			List<String> failedComponents,
			String degradationReason
	) {
		List<GovernanceSearchResult> sorted = results.stream()
				.sorted(Comparator.comparing(
						GovernanceSearchResult::occurredAt,
						Comparator.nullsLast(Comparator.naturalOrder())
				).reversed())
				.limit(limit)
				.toList();

		GovernanceSearchDegradation degradation =
				failedComponents == null || failedComponents.isEmpty()
						? GovernanceSearchDegradation.none()
						: GovernanceSearchDegradation.partial(failedComponents, degradationReason);

		return new GovernanceSearchResponse(
				Instant.now(),
				normalizedQuery,
				type,
				limit,
				sorted,
				degradation
		);
	}

	private String reasonFor(Throwable ex) {
		return ex instanceof TimeoutException
				? "component_query_timeout"
				: "component_query_failed";
	}

	private List<GovernanceSearchResult> recommendationResults(
			List<RecommendationRecord> records,
			GovernanceDashboardTimeRange range,
			String query
	) {
		if (records == null || records.isEmpty()) {
			return List.of();
		}

		return records.stream()
				.filter(record -> range.contains(record.generatedAt()))
				.filter(record -> matches(
						query,
						record.recommendationRecordId(),
						record.incidentId(),
						record.status(),
						record.service(),
						record.domain(),
						record.policyDecision(),
						record.guardrailDecision()
				))
				.map(record -> new GovernanceSearchResult(
						GovernanceSearchType.RECOMMENDATION,
						record.recommendationRecordId(),
						sanitizer.safeText("Recommendation " + record.recommendationRecordId()),
						sanitizer.safeStatus(record.policyDecision()),
						sanitizer.safeText(record.service() + " / " + record.domain()),
						record.generatedAt(),
						"/internal/governance/details/recommendations/" + record.recommendationRecordId(),
						"/internal/governance/details/overview/recommendations/" + record.recommendationRecordId()
				))
				.toList();
	}

	private List<GovernanceSearchResult> incidentResults(
			List<IncidentLifecycleRecord> records,
			GovernanceDashboardTimeRange range,
			String query
	) {
		if (records == null || records.isEmpty()) {
			return List.of();
		}

		Map<String, IncidentLifecycleRecord> latestByIncident = records.stream()
				.filter(record -> range.contains(record.transitionedAt()))
				.collect(java.util.stream.Collectors.toMap(
						IncidentLifecycleRecord::incidentId,
						Function.identity(),
						(left, right) -> left.transitionedAt().isAfter(right.transitionedAt())
								? left
								: right
				));

		return latestByIncident.values().stream()
				.filter(record -> matches(
						query,
						record.incidentId(),
						record.currentStatus() == null ? null : record.currentStatus().name(),
						record.summary()
				))
				.map(record -> new GovernanceSearchResult(
						GovernanceSearchType.INCIDENT,
						record.incidentId(),
						sanitizer.safeText("Incident " + record.incidentId()),
						sanitizer.safeStatus(record.currentStatus()),
						sanitizer.safeText(record.summary()),
						record.transitionedAt(),
						"/internal/governance/details/incidents/" + record.incidentId(),
						"/internal/governance/details/overview/incidents/" + record.incidentId()
				))
				.toList();
	}

	private List<GovernanceSearchResult> learningResults(
			List<LearningCandidateRecord> records,
			GovernanceDashboardTimeRange range,
			String query
	) {
		if (records == null || records.isEmpty()) {
			return List.of();
		}

		return records.stream()
				.filter(record -> range.contains(record.createdAt()))
				.filter(record -> matches(
						query,
						record.learningCandidateId(),
						record.incidentId(),
						record.type() == null ? null : record.type().name(),
						record.status() == null ? null : record.status().name(),
						record.summary()
				))
				.map(record -> new GovernanceSearchResult(
						GovernanceSearchType.LEARNING_CANDIDATE,
						record.learningCandidateId(),
						sanitizer.safeText("Learning candidate " + record.learningCandidateId()),
						sanitizer.safeStatus(record.status()),
						sanitizer.safeText(record.summary()),
						record.createdAt(),
						"/internal/governance/details/learning-candidates/" + record.learningCandidateId(),
						"/internal/governance/details/overview/learning-candidates/" + record.learningCandidateId()
				))
				.toList();
	}

	private List<GovernanceSearchResult> knowledgeUpdateResults(
			List<KnowledgeUpdateApplicationRecord> records,
			GovernanceDashboardTimeRange range,
			String query
	) {
		if (records == null || records.isEmpty()) {
			return List.of();
		}

		return records.stream()
				.filter(record -> range.contains(record.appliedAt()))
				.filter(record -> matches(
						query,
						record.knowledgeUpdateApplicationId(),
						record.incidentId(),
						record.learningCandidateId(),
						record.filePath(),
						record.knowledgeType(),
						record.knowledgeLayer() == null ? null : record.knowledgeLayer().name(),
						record.changeType() == null ? null : record.changeType().name()
				))
				.map(record -> new GovernanceSearchResult(
						GovernanceSearchType.KNOWLEDGE_UPDATE,
						record.knowledgeUpdateApplicationId(),
						sanitizer.safeText("Knowledge update " + record.knowledgeUpdateApplicationId()),
						sanitizer.safeStatus(record.changeType()),
						sanitizer.safeText(record.filePath()),
						record.appliedAt(),
						"/internal/governance/details/knowledge-updates/" + record.knowledgeUpdateApplicationId(),
						"/internal/governance/details/overview/knowledge-updates/" + record.knowledgeUpdateApplicationId()
				))
				.toList();
	}

	private boolean matches(String query, String... values) {
		if (query == null || query.isBlank()) {
			return true;
		}

		String normalizedQuery = query.toLowerCase(Locale.ROOT);
		for (String value : values) {
			if (value != null && value.toLowerCase(Locale.ROOT).contains(normalizedQuery)) {
				return true;
			}
		}
		return false;
	}
}
