package com.fintech.sre.agent.governance.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.governance.detail.GovernanceDetailSanitizer;
import com.fintech.sre.agent.incident.lifecycle.InMemoryIncidentLifecycleStore;
import com.fintech.sre.agent.incident.lifecycle.IncidentLifecycleRecord;
import com.fintech.sre.agent.incident.lifecycle.IncidentStatus;
import com.fintech.sre.agent.incident.lifecycle.IncidentTransitionReason;
import com.fintech.sre.agent.learning.application.InMemoryKnowledgeUpdateApplicationStore;
import com.fintech.sre.agent.learning.application.KnowledgeUpdateApplicationRecord;
import com.fintech.sre.agent.learning.application.KnowledgeUpdateChangeType;
import com.fintech.sre.agent.learning.application.KnowledgeUpdateLayer;
import com.fintech.sre.agent.learning.candidate.InMemoryLearningCandidateStore;
import com.fintech.sre.agent.learning.candidate.LearningCandidateRecord;
import com.fintech.sre.agent.learning.candidate.LearningCandidateStatus;
import com.fintech.sre.agent.learning.candidate.LearningCandidateType;
import com.fintech.sre.agent.observability.metrics.GovernanceMetricsRecorder;
import com.fintech.sre.agent.recommendation.persistence.InMemoryRecommendationRecordStore;
import com.fintech.sre.agent.recommendation.persistence.RecommendationRecord;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class GovernanceSearchServiceTest {

	@Test
	void shouldSearchAcrossAllTypes() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceSearchService service = serviceWithSampleData(registry);

		GovernanceSearchResponse response = service.search(
				new GovernanceSearchQuery("incident-1", GovernanceSearchType.ALL, "24h", 20)
		).block();

		assertThat(response.degradation()).isEqualTo(GovernanceSearchDegradation.none());
		assertThat(response.results()).extracting(GovernanceSearchResult::type)
				.contains(
						GovernanceSearchType.INCIDENT,
						GovernanceSearchType.RECOMMENDATION,
						GovernanceSearchType.LEARNING_CANDIDATE,
						GovernanceSearchType.KNOWLEDGE_UPDATE
				);
		assertThat(registry.find(GovernanceSearchMetricName.QUERY_TOTAL)
				.tag("type", "ALL")
				.tag("result", "success")
				.counter()
				.count()).isEqualTo(1.0);
		assertThat(registry.find(GovernanceSearchMetricName.DEGRADED_TOTAL)
				.tag("type", "ALL")
				.counter()).isNull();
	}

	@Test
	void shouldFilterByType() {
		GovernanceSearchService service = serviceWithSampleData(new SimpleMeterRegistry());

		GovernanceSearchResponse response = service.search(
				new GovernanceSearchQuery("payment", GovernanceSearchType.RECOMMENDATION, "24h", 20)
		).block();

		assertThat(response.degradation()).isEqualTo(GovernanceSearchDegradation.none());
		assertThat(response.results()).isNotEmpty();
		assertThat(response.results()).allSatisfy(result ->
				assertThat(result.type()).isEqualTo(GovernanceSearchType.RECOMMENDATION));
	}

	@Test
	void shouldRespectSafeLimit() {
		GovernanceSearchService service = serviceWithSampleData(new SimpleMeterRegistry());

		GovernanceSearchResponse response = service.search(
				new GovernanceSearchQuery("", GovernanceSearchType.ALL, "24h", 1)
		).block();

		assertThat(response.limit()).isEqualTo(1);
		assertThat(response.results()).hasSize(1);
	}

	@Test
	void shouldAllowBlankQuery() {
		GovernanceSearchService service = serviceWithSampleData(new SimpleMeterRegistry());

		GovernanceSearchResponse response = service.search(
				new GovernanceSearchQuery("   ", GovernanceSearchType.ALL, "24h", 20)
		).block();

		assertThat(response.results()).isNotEmpty();
	}

	@Test
	void shouldBuildDetailAndOverviewPaths() {
		GovernanceSearchService service = serviceWithSampleData(new SimpleMeterRegistry());

		GovernanceSearchResponse response = service.search(
				new GovernanceSearchQuery("update-1", GovernanceSearchType.KNOWLEDGE_UPDATE, "24h", 20)
		).block();

		assertThat(response.results()).singleElement().satisfies(result -> {
			assertThat(result.detailPath())
					.isEqualTo("/internal/governance/details/knowledge-updates/update-1");
			assertThat(result.overviewPath())
					.isEqualTo("/internal/governance/details/overview/knowledge-updates/update-1");
		});
	}

	@Test
	void shouldSanitizeSensitiveText() {
		GovernanceSearchService service = serviceWithSampleData(new SimpleMeterRegistry());

		GovernanceSearchResponse response = service.search(
				new GovernanceSearchQuery("update-1", GovernanceSearchType.KNOWLEDGE_UPDATE, "24h", 20)
		).block();

		assertThat(response.results()).singleElement().satisfies(result ->
				assertThat(result.summary()).isEqualTo("[redacted]"));
	}

	@Test
	void shouldRecordEmptyMetricWhenNoResultsMatch() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceSearchService service = serviceWithSampleData(registry);

		GovernanceSearchResponse response = service.search(
				new GovernanceSearchQuery("does-not-match", GovernanceSearchType.ALL, "24h", 20)
		).block();

		assertThat(response.degradation()).isEqualTo(GovernanceSearchDegradation.none());
		assertThat(response.results()).isEmpty();
		assertThat(registry.find(GovernanceSearchMetricName.QUERY_TOTAL)
				.tag("type", "ALL")
				.tag("result", "empty")
				.counter()
				.count()).isEqualTo(1.0);
		assertThat(registry.find(GovernanceSearchMetricName.RESULT_COUNT)
				.tag("type", "ALL")
				.summary()
				.totalAmount()).isEqualTo(0.0);
	}

	@Test
	void shouldRecordFailureMetricWhenSearchFails() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceSearchService service = new GovernanceSearchService(
				new InMemoryRecommendationRecordStore(),
				new InMemoryIncidentLifecycleStore() {
					@Override
					public reactor.core.publisher.Flux<IncidentLifecycleRecord> findRecent(int limit) {
						return reactor.core.publisher.Flux.error(new IllegalStateException("boom"));
					}
				},
				new InMemoryLearningCandidateStore(),
				new InMemoryKnowledgeUpdateApplicationStore(),
				new GovernanceDetailSanitizer(),
				new GovernanceSearchMetricsRecorder(
						new GovernanceMetricsRecorder(registry),
						registry
				),
				resilience()
		);

		assertThatThrownBy(() -> service.search(
				new GovernanceSearchQuery("incident", GovernanceSearchType.ALL, "24h", 20)
		).block()).isInstanceOf(IllegalStateException.class);

		assertThat(registry.find(GovernanceSearchMetricName.QUERY_TOTAL)
				.tag("type", "ALL")
				.tag("result", "failure")
				.counter()
				.count()).isEqualTo(1.0);
		assertThat(registry.find(GovernanceSearchMetricName.QUERY_TOTAL)
				.tag("q", "incident")
				.counter()).isNull();
		assertThat(registry.find(GovernanceSearchMetricName.QUERY_TOTAL)
				.tag("recordId", "incident-1")
				.counter()).isNull();
	}

	@Test
	void shouldReturnPartialDegradedResultsWhenAllSearchComponentFails() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceSearchService service = serviceWithSampleData(
				registry,
				new InMemoryIncidentLifecycleStore() {
					@Override
					public reactor.core.publisher.Flux<IncidentLifecycleRecord> findRecent(int limit) {
						return reactor.core.publisher.Flux.error(new IllegalStateException("boom"));
					}
				},
				resilienceEnabled()
		);

		GovernanceSearchResponse response = service.search(
				new GovernanceSearchQuery("", GovernanceSearchType.ALL, "24h", 20)
		).block();

		assertThat(response.degradation().degraded()).isTrue();
		assertThat(response.degradation().partialResult()).isTrue();
		assertThat(response.degradation().failedComponents()).containsExactly("incident");
		assertThat(response.degradation().reason()).isEqualTo("component_query_failed");
		assertThat(response.results()).extracting(GovernanceSearchResult::type)
				.containsExactlyInAnyOrder(
						GovernanceSearchType.RECOMMENDATION,
						GovernanceSearchType.LEARNING_CANDIDATE,
						GovernanceSearchType.KNOWLEDGE_UPDATE
				)
				.doesNotContain(GovernanceSearchType.INCIDENT);
		assertThat(registry.find(GovernanceSearchMetricName.QUERY_TOTAL)
				.tag("type", "ALL")
				.tag("result", "success")
				.counter()
				.count()).isEqualTo(1.0);
		assertThat(registry.find(GovernanceSearchMetricName.QUERY_TOTAL)
				.tag("type", "ALL")
				.tag("result", "failure")
				.counter()).isNull();
		assertThat(registry.find(GovernanceSearchMetricName.DEGRADED_TOTAL)
				.tag("type", "ALL")
				.tag("reason", "component_query_failed")
				.tag("component", "incident")
				.counter()
				.count()).isEqualTo(1.0);
	}

	@Test
	void shouldRecordDegradedMetricForEachFailedComponent() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceSearchService service = new GovernanceSearchService(
				new InMemoryRecommendationRecordStore() {
					@Override
					public reactor.core.publisher.Flux<RecommendationRecord> findRecent(int limit) {
						return reactor.core.publisher.Flux.error(new IllegalStateException("boom"));
					}
				},
				new InMemoryIncidentLifecycleStore() {
					@Override
					public reactor.core.publisher.Flux<IncidentLifecycleRecord> findRecent(int limit) {
						return reactor.core.publisher.Flux.error(new IllegalStateException("boom"));
					}
				},
				new InMemoryLearningCandidateStore(),
				new InMemoryKnowledgeUpdateApplicationStore(),
				new GovernanceDetailSanitizer(),
				new GovernanceSearchMetricsRecorder(
						new GovernanceMetricsRecorder(registry),
						registry
				),
				resilienceEnabled()
		);

		GovernanceSearchResponse response = service.search(
				new GovernanceSearchQuery("", GovernanceSearchType.ALL, "24h", 20)
		).block();

		assertThat(response.degradation().failedComponents())
				.containsExactlyInAnyOrder("recommendation", "incident");
		assertThat(registry.find(GovernanceSearchMetricName.DEGRADED_TOTAL)
				.tag("type", "ALL")
				.tag("reason", "component_query_failed")
				.tag("component", "recommendation")
				.counter()
				.count()).isEqualTo(1.0);
		assertThat(registry.find(GovernanceSearchMetricName.DEGRADED_TOTAL)
				.tag("type", "ALL")
				.tag("reason", "component_query_failed")
				.tag("component", "incident")
				.counter()
				.count()).isEqualTo(1.0);
	}

	@Test
	void shouldPropagateFailureWhenPartialSearchDisabled() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceSearchService service = new GovernanceSearchService(
				new InMemoryRecommendationRecordStore(),
				new InMemoryIncidentLifecycleStore() {
					@Override
					public reactor.core.publisher.Flux<IncidentLifecycleRecord> findRecent(int limit) {
						return reactor.core.publisher.Flux.error(new IllegalStateException("boom"));
					}
				},
				new InMemoryLearningCandidateStore(),
				new InMemoryKnowledgeUpdateApplicationStore(),
				new GovernanceDetailSanitizer(),
				new GovernanceSearchMetricsRecorder(
						new GovernanceMetricsRecorder(registry),
						registry
				),
				partialDisabled()
		);

		assertThatThrownBy(() -> service.search(
				new GovernanceSearchQuery("", GovernanceSearchType.ALL, "24h", 20)
		).block()).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void shouldPropagateFailureWhenFailOpenSearchDisabled() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceSearchService service = new GovernanceSearchService(
				new InMemoryRecommendationRecordStore(),
				new InMemoryIncidentLifecycleStore() {
					@Override
					public reactor.core.publisher.Flux<IncidentLifecycleRecord> findRecent(int limit) {
						return reactor.core.publisher.Flux.error(new IllegalStateException("boom"));
					}
				},
				new InMemoryLearningCandidateStore(),
				new InMemoryKnowledgeUpdateApplicationStore(),
				new GovernanceDetailSanitizer(),
				new GovernanceSearchMetricsRecorder(
						new GovernanceMetricsRecorder(registry),
						registry
				),
				failOpenDisabled()
		);

		assertThatThrownBy(() -> service.search(
				new GovernanceSearchQuery("", GovernanceSearchType.ALL, "24h", 20)
		).block()).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void shouldKeepSingleTypeSearchStrict() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceSearchService service = new GovernanceSearchService(
				new InMemoryRecommendationRecordStore(),
				new InMemoryIncidentLifecycleStore() {
					@Override
					public reactor.core.publisher.Flux<IncidentLifecycleRecord> findRecent(int limit) {
						return reactor.core.publisher.Flux.error(new IllegalStateException("boom"));
					}
				},
				new InMemoryLearningCandidateStore(),
				new InMemoryKnowledgeUpdateApplicationStore(),
				new GovernanceDetailSanitizer(),
				new GovernanceSearchMetricsRecorder(
						new GovernanceMetricsRecorder(registry),
						registry
				),
				resilienceEnabled()
		);

		assertThatThrownBy(() -> service.search(
				new GovernanceSearchQuery("", GovernanceSearchType.INCIDENT, "24h", 20)
		).block()).isInstanceOf(IllegalStateException.class);
	}

	private GovernanceSearchService serviceWithSampleData(SimpleMeterRegistry registry) {
		return serviceWithSampleData(
				registry,
				new InMemoryIncidentLifecycleStore(),
				resilience()
		);
	}

	private GovernanceSearchService serviceWithSampleData(
			SimpleMeterRegistry registry,
			InMemoryIncidentLifecycleStore incidentStore,
			GovernanceSearchResilienceProperties resilienceProperties
	) {
		Instant base = Instant.now().minusSeconds(300);
		InMemoryRecommendationRecordStore recommendationStore = new InMemoryRecommendationRecordStore();
		InMemoryLearningCandidateStore learningStore = new InMemoryLearningCandidateStore();
		InMemoryKnowledgeUpdateApplicationStore updateStore = new InMemoryKnowledgeUpdateApplicationStore();

		recommendationStore.save(new RecommendationRecord(
				"rec-1",
				"incident-1",
				"audit-1",
				"PROMETHEUS_ALERTMANAGER",
				"payment-api",
				"payment",
				"CRITICAL",
				"firing",
				base.plusSeconds(10),
				1,
				0,
				"ALLOW",
				"PASS",
				List.of("RATE_LIMIT"),
				List.of(),
				Map.of()
		)).block();
		incidentStore.save(new IncidentLifecycleRecord(
				"lifecycle-1",
				"incident-1",
				IncidentStatus.OPEN,
				IncidentStatus.MITIGATING,
				IncidentTransitionReason.MANUAL_ESCALATION,
				"operator-a",
				"payment incident under mitigation",
				base.plusSeconds(20),
				Map.of()
		)).block();
		learningStore.save(new LearningCandidateRecord(
				"candidate-1",
				"incident-1",
				"draft-1",
				"review-1",
				LearningCandidateType.RUNBOOK_UPDATE,
				LearningCandidateStatus.REVIEW_REQUIRED,
				"operator-a",
				"payment runbook update",
				List.of("runbook update"),
				base.plusSeconds(30),
				Map.of()
		)).block();
		updateStore.save(new KnowledgeUpdateApplicationRecord(
				"update-1",
				"incident-1",
				"candidate-1",
				"promotion-plan-1",
				"RUNBOOK",
				KnowledgeUpdateLayer.PRIMARY_OPERATIONAL_KNOWLEDGE,
				"runbooks/payment.md",
				KnowledgeUpdateChangeType.UPDATED,
				"portfolio",
				"main",
				"abc123",
				"PR-101",
				"operator-a",
				"reviewer-a",
				"approver-a",
				List.of("link-check"),
				base.plusSeconds(40),
				Map.of("secret", "hidden")
		)).block();

		return new GovernanceSearchService(
				recommendationStore,
				incidentStore,
				learningStore,
				updateStore,
				new GovernanceDetailSanitizer(),
				new GovernanceSearchMetricsRecorder(
						new GovernanceMetricsRecorder(registry),
						registry
				),
				resilienceProperties
		);
	}

	private GovernanceSearchResilienceProperties resilience() {
		return new GovernanceSearchResilienceProperties();
	}

	private GovernanceSearchResilienceProperties resilienceEnabled() {
		GovernanceSearchResilienceProperties properties =
				new GovernanceSearchResilienceProperties();
		properties.setEnabled(true);
		return properties;
	}

	private GovernanceSearchResilienceProperties partialDisabled() {
		GovernanceSearchResilienceProperties properties = resilienceEnabled();
		properties.setPartialSearchEnabled(false);
		return properties;
	}

	private GovernanceSearchResilienceProperties failOpenDisabled() {
		GovernanceSearchResilienceProperties properties = resilienceEnabled();
		properties.setFailOpenSearch(false);
		return properties;
	}
}
