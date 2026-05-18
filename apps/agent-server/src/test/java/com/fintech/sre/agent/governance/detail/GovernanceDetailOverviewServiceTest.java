package com.fintech.sre.agent.governance.detail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.fintech.sre.agent.observability.metrics.GovernanceMetricsRecorder;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import reactor.core.publisher.Mono;

class GovernanceDetailOverviewServiceTest {

	private final GovernanceDetailOverviewBuilder builder =
			new GovernanceDetailOverviewBuilder(new GovernanceDetailSanitizer());

	@Test
	void shouldBuildIncidentOverview() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceDetailOverviewService service = new GovernanceDetailOverviewService(
				incidentService(incidentResponse()),
				recommendationService(recommendationResponse()),
				learningService(learningResponse()),
				knowledgeUpdateService(knowledgeUpdateResponse()),
				builder,
				recorder(registry)
		);

		GovernanceDetailOverviewResponse response = service.incidentOverview("incident-1").block();

		assertThat(response.type()).isEqualTo(GovernanceDetailType.INCIDENT);
		assertThat(response.recordId()).isEqualTo("incident-1");
		assertThat(response.status()).isEqualTo("MITIGATING");
		assertThat(response.counts().recommendations()).isEqualTo(2);
		assertThat(response.counts().verifications()).isEqualTo(1);
		assertThat(response.counts().postmortems()).isEqualTo(2);
		assertThat(response.latestTimeline().type()).isEqualTo("KNOWLEDGE_UPDATED");
		assertThat(response.degradation()).isEqualTo(GovernanceDetailDegradation.none());
		assertThat(registry.find(GovernanceDetailOverviewMetricName.QUERY_TOTAL)
				.tag("detailType", "incident")
				.tag("result", "success")
				.counter()
				.count()).isEqualTo(1.0);
		assertThat(registry.find(GovernanceDetailOverviewMetricName.DEGRADED_TOTAL)
				.tag("detailType", "incident")
				.counter()).isNull();
	}

	@Test
	void shouldBuildRecommendationOverviewWithDegradation() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceDetailOverviewService service = new GovernanceDetailOverviewService(
				incidentService(incidentResponse()),
				recommendationService(withDegradation(
						recommendationResponse(),
						GovernanceDetailDegradation.partial(List.of("approvals"), "component_query_failed")
				)),
				learningService(learningResponse()),
				knowledgeUpdateService(knowledgeUpdateResponse()),
				builder,
				recorder(registry)
		);

		GovernanceDetailOverviewResponse response = service.recommendationOverview("rec-1").block();

		assertThat(response.type()).isEqualTo(GovernanceDetailType.RECOMMENDATION);
		assertThat(response.counts().approvals()).isEqualTo(1);
		assertThat(response.counts().executionPlans()).isEqualTo(1);
		assertThat(response.latestTimeline().type()).isEqualTo("VERIFICATION_RECORDED");
		assertThat(response.degradation().degraded()).isTrue();
		assertThat(response.degradation().failedComponents()).containsExactly("approvals");
		assertThat(registry.find(GovernanceDetailOverviewMetricName.QUERY_TOTAL)
				.tag("detailType", "recommendation")
				.tag("result", "success")
				.counter()
				.count()).isEqualTo(1.0);
		assertThat(registry.find(GovernanceDetailOverviewMetricName.DEGRADED_TOTAL)
				.tag("detailType", "recommendation")
				.tag("reason", "component_query_failed")
				.counter()
				.count()).isEqualTo(1.0);
	}

	@Test
	void shouldBuildLearningOverview() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceDetailOverviewService service = new GovernanceDetailOverviewService(
				incidentService(incidentResponse()),
				recommendationService(recommendationResponse()),
				learningService(learningResponse()),
				knowledgeUpdateService(knowledgeUpdateResponse()),
				builder,
				recorder(registry)
		);

		GovernanceDetailOverviewResponse response = service.learningOverview("candidate-1").block();

		assertThat(response.type()).isEqualTo(GovernanceDetailType.LEARNING);
		assertThat(response.counts().approvals()).isEqualTo(2);
		assertThat(response.counts().executionPlans()).isEqualTo(1);
		assertThat(response.counts().knowledgeUpdates()).isEqualTo(1);
		assertThat(response.latestTimeline().type()).isEqualTo("KNOWLEDGE_UPDATED");
		assertThat(registry.find(GovernanceDetailOverviewMetricName.QUERY_TOTAL)
				.tag("detailType", "learningCandidate")
				.tag("result", "success")
				.counter()
				.count()).isEqualTo(1.0);
	}

	@Test
	void shouldBuildKnowledgeUpdateOverview() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceDetailOverviewService service = new GovernanceDetailOverviewService(
				incidentService(incidentResponse()),
				recommendationService(recommendationResponse()),
				learningService(learningResponse()),
				knowledgeUpdateService(knowledgeUpdateResponse()),
				builder,
				recorder(registry)
		);

		GovernanceDetailOverviewResponse response = service.knowledgeUpdateOverview("update-1").block();

		assertThat(response.type()).isEqualTo(GovernanceDetailType.KNOWLEDGE_UPDATE);
		assertThat(response.status()).isEqualTo("UPDATED");
		assertThat(response.counts().approvals()).isEqualTo(2);
		assertThat(response.counts().executionPlans()).isEqualTo(1);
		assertThat(response.counts().learningCandidates()).isEqualTo(1);
		assertThat(response.latestTimeline().summary()).isEqualTo("[redacted]");
		assertThat(registry.find(GovernanceDetailOverviewMetricName.QUERY_TOTAL)
				.tag("detailType", "knowledgeUpdate")
				.tag("result", "success")
				.counter()
				.count()).isEqualTo(1.0);
	}

	@Test
	void shouldPropagateNotFound() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceDetailOverviewService service = new GovernanceDetailOverviewService(
				missingIncidentService(),
				recommendationService(recommendationResponse()),
				learningService(learningResponse()),
				knowledgeUpdateService(knowledgeUpdateResponse()),
				builder,
				recorder(registry)
		);

		assertThatThrownBy(() -> service.incidentOverview("missing").block())
				.isInstanceOfSatisfying(ResponseStatusException.class, ex ->
						assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
		assertThat(registry.find(GovernanceDetailOverviewMetricName.QUERY_TOTAL)
				.tag("detailType", "incident")
				.tag("result", "not_found")
				.counter()
				.count()).isEqualTo(1.0);
	}

	@Test
	void shouldRecordFailureMetricWhenOverviewQueryFails() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceDetailOverviewService service = new GovernanceDetailOverviewService(
				failingIncidentService(),
				recommendationService(recommendationResponse()),
				learningService(learningResponse()),
				knowledgeUpdateService(knowledgeUpdateResponse()),
				builder,
				recorder(registry)
		);

		assertThatThrownBy(() -> service.incidentOverview("incident-1").block())
				.isInstanceOf(IllegalStateException.class);
		assertThat(registry.find(GovernanceDetailOverviewMetricName.QUERY_TOTAL)
				.tag("detailType", "incident")
				.tag("result", "failure")
				.counter()
				.count()).isEqualTo(1.0);
	}

	private GovernanceIncidentDetailService incidentService(
			GovernanceIncidentDetailResponse response
	) {
		return new GovernanceIncidentDetailService(
				null, null, null, null, null, null, null, null, null, null, null, null, null, null
		) {
			@Override
			public Mono<GovernanceIncidentDetailResponse> findByIncidentId(String incidentId) {
				return Mono.just(response);
			}
		};
	}

	private GovernanceIncidentDetailService missingIncidentService() {
		return new GovernanceIncidentDetailService(
				null, null, null, null, null, null, null, null, null, null, null, null, null, null
		) {
			@Override
			public Mono<GovernanceIncidentDetailResponse> findByIncidentId(String incidentId) {
				return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND));
			}
		};
	}

	private GovernanceIncidentDetailService failingIncidentService() {
		return new GovernanceIncidentDetailService(
				null, null, null, null, null, null, null, null, null, null, null, null, null, null
		) {
			@Override
			public Mono<GovernanceIncidentDetailResponse> findByIncidentId(String incidentId) {
				return Mono.error(new IllegalStateException("boom"));
			}
		};
	}

	private GovernanceRecommendationDetailService recommendationService(
			GovernanceRecommendationDetailResponse response
	) {
		return new GovernanceRecommendationDetailService(
				null, null, null, null, null, null, null, null, null
		) {
			@Override
			public Mono<GovernanceRecommendationDetailResponse> findByRecommendationRecordId(
					String recommendationRecordId
			) {
				return Mono.just(response);
			}
		};
	}

	private GovernanceLearningDetailService learningService(
			GovernanceLearningDetailResponse response
	) {
		return new GovernanceLearningDetailService(
				null, null, null, null, null, null, null, null
		) {
			@Override
			public Mono<GovernanceLearningDetailResponse> findByLearningCandidateId(
					String learningCandidateId
			) {
				return Mono.just(response);
			}
		};
	}

	private GovernanceKnowledgeUpdateDetailService knowledgeUpdateService(
			GovernanceKnowledgeUpdateDetailResponse response
	) {
		return new GovernanceKnowledgeUpdateDetailService(
				null, null, null, null, null, null, null, null
		) {
			@Override
			public Mono<GovernanceKnowledgeUpdateDetailResponse> findByKnowledgeUpdateApplicationId(
					String knowledgeUpdateApplicationId
			) {
				return Mono.just(response);
			}
		};
	}

	private GovernanceIncidentDetailResponse incidentResponse() {
		return new GovernanceIncidentDetailResponse(
				Instant.parse("2026-05-11T00:00:00Z"),
				GovernanceDetailType.INCIDENT,
				"incident-1",
				new GovernanceDetailSummary(
						GovernanceDetailType.INCIDENT,
						"incident-1",
						"incident-1",
						"MITIGATING",
						"Incident incident-1",
						"aggregate",
						Instant.parse("2026-05-11T00:00:00Z")
				),
				"MITIGATING",
				GovernanceDetailDegradation.none(),
				List.of(
						item("INCIDENT_TRANSITIONED", "OPEN", "opened", "2026-05-11T00:00:00Z"),
						item("KNOWLEDGE_UPDATED", "UPDATED", "runbooks/payment.md", "2026-05-11T00:10:00Z")
				),
				List.of(summary("rec-1"), summary("rec-2")),
				List.of(summary("approval-1")),
				List.of(summary("plan-1")),
				List.of(summary("result-1")),
				List.of(summary("verification-1")),
				List.of(summary("draft-1")),
				List.of(summary("review-1")),
				List.of(summary("candidate-1")),
				List.of(summary("update-1"))
		);
	}

	private GovernanceRecommendationDetailResponse recommendationResponse() {
		return new GovernanceRecommendationDetailResponse(
				Instant.parse("2026-05-11T00:00:00Z"),
				GovernanceDetailType.RECOMMENDATION,
				"rec-1",
				"incident-1",
				summary("rec-1"),
				GovernanceDetailDegradation.none(),
				new GovernanceDetailSummary(
						GovernanceDetailType.RECOMMENDATION,
						"rec-1",
						"incident-1",
						"APPROVED",
						"Recommendation rec-1",
						"payment-api / payment",
						Instant.parse("2026-05-11T00:00:00Z")
				),
				List.of(summary("approval-1")),
				List.of(summary("plan-1")),
				List.of(summary("result-1")),
				List.of(summary("verification-1")),
				List.of(
						item("RECOMMENDATION_CREATED", "APPROVED", "created", "2026-05-11T00:00:00Z"),
						item("VERIFICATION_RECORDED", "VERIFIED", "verified", "2026-05-11T00:10:00Z")
				)
		);
	}

	private GovernanceLearningDetailResponse learningResponse() {
		return new GovernanceLearningDetailResponse(
				Instant.parse("2026-05-11T00:00:00Z"),
				GovernanceDetailType.LEARNING,
				"candidate-1",
				"incident-1",
				GovernanceDetailDegradation.none(),
				new GovernanceDetailSummary(
						GovernanceDetailType.LEARNING,
						"candidate-1",
						"incident-1",
						"REVIEW_REQUIRED",
						"Learning candidate candidate-1",
						"candidate",
						Instant.parse("2026-05-11T00:00:00Z")
				),
				List.of(summary("promotion-review-1"), summary("promotion-review-2")),
				List.of(summary("promotion-plan-1")),
				List.of(summary("update-1")),
				List.of(
						item("LEARNING_CANDIDATE_CREATED", "REVIEW_REQUIRED", "created", "2026-05-11T00:00:00Z"),
						item("KNOWLEDGE_UPDATED", "UPDATED", "runbooks/payment.md", "2026-05-11T00:10:00Z")
				)
		);
	}

	private GovernanceKnowledgeUpdateDetailResponse knowledgeUpdateResponse() {
		return new GovernanceKnowledgeUpdateDetailResponse(
				Instant.parse("2026-05-11T00:00:00Z"),
				GovernanceDetailType.KNOWLEDGE_UPDATE,
				"update-1",
				"incident-1",
				summary("update-1"),
				GovernanceDetailDegradation.none(),
				new GovernanceDetailSummary(
						GovernanceDetailType.KNOWLEDGE_UPDATE,
						"update-1",
						"incident-1",
						"UPDATED",
						"Knowledge update update-1",
						"runbooks/payment.md",
						Instant.parse("2026-05-11T00:10:00Z")
				),
				new GovernanceDetailSummary(
						GovernanceDetailType.LEARNING,
						"candidate-1",
						"incident-1",
						"REVIEW_REQUIRED",
						"Learning candidate candidate-1",
						"candidate",
						Instant.parse("2026-05-11T00:00:00Z")
				),
				new GovernanceDetailSummary(
						GovernanceDetailType.LEARNING,
						"promotion-plan-1",
						"incident-1",
						"PLAN_CREATED",
						"Promotion plan promotion-plan-1",
						"plan",
						Instant.parse("2026-05-11T00:05:00Z")
				),
				List.of(summary("promotion-review-1"), summary("promotion-review-2")),
				"RUNBOOK",
				"PRIMARY_OPERATIONAL_KNOWLEDGE",
				"runbooks/payment.md",
				"UPDATED",
				"portfolio",
				"main",
				"abc123",
				"PR-101",
				List.of("link-check"),
				List.of(
						item("PROMOTION_REVIEWED", "APPROVED_FOR_PROMOTION", "reviewed", "2026-05-11T00:03:00Z"),
						item("PROMOTION_PLAN_CREATED", "PLAN_CREATED", "planned", "2026-05-11T00:05:00Z"),
						item("KNOWLEDGE_UPDATED", "UPDATED", "runbooks/payment.md", "2026-05-11T00:10:00Z")
				)
		);
	}

	private GovernanceDetailSummary summary(String id) {
		return new GovernanceDetailSummary(
				GovernanceDetailType.RECOMMENDATION,
				id,
				"incident-1",
				"OK",
				id,
				"summary",
				Instant.parse("2026-05-11T00:00:00Z")
		);
	}

	private GovernanceDetailTimelineItem item(
			String type,
			String status,
			String summary,
			String occurredAt
	) {
		return new GovernanceDetailTimelineItem(
				Instant.parse(occurredAt),
				type,
				"record",
				status,
				type,
				summary
		);
	}

	private GovernanceRecommendationDetailResponse withDegradation(
			GovernanceRecommendationDetailResponse delegate,
			GovernanceDetailDegradation degradation
	) {
		return new GovernanceRecommendationDetailResponse(
				delegate.generatedAt(),
				delegate.type(),
				delegate.recommendationRecordId(),
				delegate.incidentId(),
				delegate.summary(),
				degradation,
				delegate.recommendation(),
				delegate.approvals(),
				delegate.executionPlans(),
				delegate.humanExecutionResults(),
				delegate.verifications(),
				delegate.timeline()
		);
	}

	private GovernanceDetailOverviewMetricsRecorder recorder(
			SimpleMeterRegistry registry
	) {
		return new GovernanceDetailOverviewMetricsRecorder(
				new GovernanceMetricsRecorder(registry)
		);
	}
}
