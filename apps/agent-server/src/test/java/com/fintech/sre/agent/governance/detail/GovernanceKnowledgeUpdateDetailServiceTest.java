package com.fintech.sre.agent.governance.detail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import com.fintech.sre.agent.learning.application.InMemoryKnowledgeUpdateApplicationStore;
import com.fintech.sre.agent.learning.application.KnowledgeUpdateApplicationRecord;
import com.fintech.sre.agent.learning.application.KnowledgeUpdateApplicationStore;
import com.fintech.sre.agent.learning.application.KnowledgeUpdateChangeType;
import com.fintech.sre.agent.learning.application.KnowledgeUpdateLayer;
import com.fintech.sre.agent.learning.candidate.InMemoryLearningCandidateStore;
import com.fintech.sre.agent.learning.candidate.LearningCandidateRecord;
import com.fintech.sre.agent.learning.candidate.LearningCandidateStatus;
import com.fintech.sre.agent.learning.candidate.LearningCandidateType;
import com.fintech.sre.agent.learning.plan.InMemoryKnowledgePromotionPlanStore;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanRecord;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanStatus;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanStore;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanTarget;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionTargetType;
import com.fintech.sre.agent.learning.promotion.InMemoryKnowledgePromotionReviewStore;
import com.fintech.sre.agent.learning.promotion.KnowledgePromotionReviewRecord;
import com.fintech.sre.agent.learning.promotion.KnowledgePromotionReviewStatus;
import com.fintech.sre.agent.learning.promotion.KnowledgePromotionReviewStore;
import com.fintech.sre.agent.observability.metrics.GovernanceMetricsRecorder;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class GovernanceKnowledgeUpdateDetailServiceTest {

	@Test
	void shouldBuildKnowledgeUpdateDetailAggregate() {
		Instant base = Instant.parse("2026-05-11T00:00:00Z");
		InMemoryKnowledgeUpdateApplicationStore updateStore =
				new InMemoryKnowledgeUpdateApplicationStore();
		InMemoryLearningCandidateStore candidateStore =
				new InMemoryLearningCandidateStore();
		InMemoryKnowledgePromotionPlanStore planStore =
				new InMemoryKnowledgePromotionPlanStore();
		InMemoryKnowledgePromotionReviewStore reviewStore =
				new InMemoryKnowledgePromotionReviewStore();
		SimpleMeterRegistry registry = new SimpleMeterRegistry();

		candidateStore.save(candidate(base.plusSeconds(10))).block();
		reviewStore.save(review("promotion-review-1", base.plusSeconds(20), "review ok")).block();
		reviewStore.save(review("promotion-review-2", base.plusSeconds(25), "token in review")).block();
		planStore.save(plan(base.plusSeconds(30))).block();
		updateStore.save(update(base.plusSeconds(40))).block();

		GovernanceKnowledgeUpdateDetailService service =
				service(updateStore, candidateStore, planStore, reviewStore, resilience(), registry);

		GovernanceKnowledgeUpdateDetailResponse response =
				service.findByKnowledgeUpdateApplicationId("update-1").block();

		assertThat(response.knowledgeUpdateApplicationId()).isEqualTo("update-1");
		assertThat(response.degradation()).isEqualTo(GovernanceDetailDegradation.none());
		assertThat(response.knowledgeUpdate()).isNotNull();
		assertThat(response.learningCandidate()).isNotNull();
		assertThat(response.promotionPlan()).isNotNull();
		assertThat(response.promotionReviews()).hasSize(2);
		assertThat(response.validationChecks()).containsExactly("link-check", "[redacted]");
		assertThat(response.timeline()).isSortedAccordingTo(
				java.util.Comparator.comparing(GovernanceDetailTimelineItem::occurredAt)
		);
		assertThat(response.promotionReviews())
				.anySatisfy(review -> assertThat(review.summary()).isEqualTo("[redacted]"));
		assertThat(registry.find(GovernanceDetailMetricName.QUERY_TOTAL)
				.tag("detailType", "knowledgeUpdate")
				.tag("result", "success")
				.counter()
				.count()).isEqualTo(1.0);
		assertThat(registry.find(GovernanceDetailMetricName.DEGRADED_TOTAL)
				.tag("detailType", "knowledgeUpdate")
				.counter()).isNull();
	}

	@Test
	void shouldReturnNotFoundWhenKnowledgeUpdateDoesNotExist() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceKnowledgeUpdateDetailService service = service(
				new InMemoryKnowledgeUpdateApplicationStore(),
				new InMemoryLearningCandidateStore(),
				new InMemoryKnowledgePromotionPlanStore(),
				new InMemoryKnowledgePromotionReviewStore(),
				resilience(),
				registry
		);

		assertThatThrownBy(() -> service.findByKnowledgeUpdateApplicationId("missing").block())
				.isInstanceOf(ResponseStatusException.class);
		assertThat(registry.find(GovernanceDetailMetricName.QUERY_NOT_FOUND)
				.tag("detailType", "knowledgeUpdate")
				.tag("result", "not_found")
				.counter()
				.count()).isEqualTo(1.0);
	}

	@Test
	void shouldReturnPartialDegradedKnowledgeUpdateDetailWhenChildComponentFails() {
		Instant base = Instant.parse("2026-05-11T00:00:00Z");
		InMemoryKnowledgeUpdateApplicationStore updateStore =
				new InMemoryKnowledgeUpdateApplicationStore();
		InMemoryLearningCandidateStore candidateStore =
				new InMemoryLearningCandidateStore();
		InMemoryKnowledgePromotionPlanStore planStore =
				new InMemoryKnowledgePromotionPlanStore();
		SimpleMeterRegistry registry = new SimpleMeterRegistry();

		candidateStore.save(candidate(base.plusSeconds(10))).block();
		planStore.save(plan(base.plusSeconds(30))).block();
		updateStore.save(update(base.plusSeconds(40))).block();

		GovernanceKnowledgeUpdateDetailService service = service(
				updateStore,
				candidateStore,
				planStore,
				failingReviewStore(),
				resilienceEnabled(),
				registry
		);

		GovernanceKnowledgeUpdateDetailResponse response =
				service.findByKnowledgeUpdateApplicationId("update-1").block();

		assertThat(response.degradation().degraded()).isTrue();
		assertThat(response.degradation().failedComponents()).containsExactly("promotionReviews");
		assertThat(response.learningCandidate()).isNotNull();
		assertThat(response.promotionPlan()).isNotNull();
		assertThat(response.promotionReviews()).isEmpty();
		assertThat(response.timeline().stream()
				.map(GovernanceDetailTimelineItem::type)
				.toList()).doesNotContain("PROMOTION_REVIEWED");
		assertThat(registry.find(GovernanceDetailMetricName.DEGRADED_TOTAL)
				.tag("detailType", "knowledgeUpdate")
				.tag("result", "degraded")
				.tag("reason", "component_query_failed")
				.tag("component", "promotionReviews")
				.counter()
				.count()).isEqualTo(1.0);
	}

	@Test
	void shouldPropagateKnowledgeUpdateChildFailureWhenPartialResponseDisabled() {
		Instant base = Instant.parse("2026-05-11T00:00:00Z");
		InMemoryKnowledgeUpdateApplicationStore updateStore =
				new InMemoryKnowledgeUpdateApplicationStore();
		updateStore.save(update(base.plusSeconds(40))).block();

		GovernanceKnowledgeUpdateDetailService service = service(
				updateStore,
				new InMemoryLearningCandidateStore(),
				new InMemoryKnowledgePromotionPlanStore(),
				failingReviewStore(),
				partialDisabled(),
				new SimpleMeterRegistry()
		);

		assertThatThrownBy(() -> service.findByKnowledgeUpdateApplicationId("update-1").block())
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void shouldPropagateKnowledgeUpdateChildFailureWhenFailOpenDisabled() {
		Instant base = Instant.parse("2026-05-11T00:00:00Z");
		InMemoryKnowledgeUpdateApplicationStore updateStore =
				new InMemoryKnowledgeUpdateApplicationStore();
		updateStore.save(update(base.plusSeconds(40))).block();

		GovernanceKnowledgeUpdateDetailService service = service(
				updateStore,
				new InMemoryLearningCandidateStore(),
				new InMemoryKnowledgePromotionPlanStore(),
				failingReviewStore(),
				failOpenDisabled(),
				new SimpleMeterRegistry()
		);

		assertThatThrownBy(() -> service.findByKnowledgeUpdateApplicationId("update-1").block())
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void shouldRecordFailureMetricWhenKnowledgeUpdateDetailQueryFails() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceKnowledgeUpdateDetailService service = service(
				failingKnowledgeUpdateStore(),
				new InMemoryLearningCandidateStore(),
				new InMemoryKnowledgePromotionPlanStore(),
				new InMemoryKnowledgePromotionReviewStore(),
				resilience(),
				registry
		);

		assertThatThrownBy(() -> service.findByKnowledgeUpdateApplicationId("update-1").block())
				.isInstanceOf(IllegalStateException.class);
		assertThat(registry.find(GovernanceDetailMetricName.QUERY_TOTAL)
				.tag("detailType", "knowledgeUpdate")
				.tag("result", "failure")
				.counter()
				.count()).isEqualTo(1.0);
	}

	private GovernanceKnowledgeUpdateDetailService service(
			KnowledgeUpdateApplicationStore updateStore,
			InMemoryLearningCandidateStore candidateStore,
			KnowledgePromotionPlanStore planStore,
			KnowledgePromotionReviewStore reviewStore,
			GovernanceDetailResilienceProperties properties,
			SimpleMeterRegistry registry
	) {
		return new GovernanceKnowledgeUpdateDetailService(
				updateStore,
				candidateStore,
				planStore,
				reviewStore,
				new GovernanceDetailTimelineBuilder(new GovernanceDetailSanitizer()),
				new GovernanceDetailSanitizer(),
				new GovernanceDetailComponentLoader(properties),
				recorder(registry)
		);
	}

	private KnowledgeUpdateApplicationStore failingKnowledgeUpdateStore() {
		return new KnowledgeUpdateApplicationStore() {
			@Override
			public Mono<KnowledgeUpdateApplicationRecord> save(KnowledgeUpdateApplicationRecord record) {
				return Mono.just(record);
			}

			@Override
			public Mono<KnowledgeUpdateApplicationRecord> findById(String knowledgeUpdateApplicationId) {
				return Mono.error(new IllegalStateException("boom"));
			}

			@Override
			public Flux<KnowledgeUpdateApplicationRecord> findByIncidentId(String incidentId) {
				return Flux.empty();
			}

			@Override
			public Flux<KnowledgeUpdateApplicationRecord> findByLearningCandidateId(String learningCandidateId) {
				return Flux.empty();
			}

			@Override
			public Flux<KnowledgeUpdateApplicationRecord> findRecent(int limit) {
				return Flux.empty();
			}
		};
	}

	private KnowledgePromotionReviewStore failingReviewStore() {
		return new KnowledgePromotionReviewStore() {
			@Override
			public Mono<KnowledgePromotionReviewRecord> save(KnowledgePromotionReviewRecord record) {
				return Mono.just(record);
			}

			@Override
			public Mono<KnowledgePromotionReviewRecord> findLatestByLearningCandidateId(String learningCandidateId) {
				return Mono.empty();
			}

			@Override
			public Flux<KnowledgePromotionReviewRecord> findByLearningCandidateId(String learningCandidateId) {
				return Flux.error(new IllegalStateException("boom"));
			}

			@Override
			public Flux<KnowledgePromotionReviewRecord> findByIncidentId(String incidentId) {
				return Flux.empty();
			}

			@Override
			public Flux<KnowledgePromotionReviewRecord> findRecent(int limit) {
				return Flux.empty();
			}
		};
	}

	private GovernanceDetailMetricsRecorder recorder(SimpleMeterRegistry registry) {
		return new GovernanceDetailMetricsRecorder(
				new GovernanceMetricsRecorder(registry)
		);
	}

	private GovernanceDetailResilienceProperties resilience() {
		return new GovernanceDetailResilienceProperties();
	}

	private GovernanceDetailResilienceProperties resilienceEnabled() {
		GovernanceDetailResilienceProperties properties = new GovernanceDetailResilienceProperties();
		properties.setEnabled(true);
		return properties;
	}

	private GovernanceDetailResilienceProperties partialDisabled() {
		GovernanceDetailResilienceProperties properties = resilienceEnabled();
		properties.setPartialResponseEnabled(false);
		return properties;
	}

	private GovernanceDetailResilienceProperties failOpenDisabled() {
		GovernanceDetailResilienceProperties properties = resilienceEnabled();
		properties.setFailOpenDetail(false);
		return properties;
	}

	private LearningCandidateRecord candidate(Instant occurredAt) {
		return new LearningCandidateRecord(
				"candidate-1",
				"incident-1",
				"draft-1",
				"review-1",
				LearningCandidateType.RUNBOOK_UPDATE,
				LearningCandidateStatus.REVIEW_REQUIRED,
				"operator-a",
				"candidate summary",
				List.of("runbook update"),
				occurredAt,
				Map.of()
		);
	}

	private KnowledgePromotionReviewRecord review(
			String reviewId,
			Instant occurredAt,
			String summary
	) {
		return new KnowledgePromotionReviewRecord(
				reviewId,
				"candidate-1",
				"incident-1",
				KnowledgePromotionReviewStatus.APPROVED_FOR_PROMOTION,
				"reviewer-a",
				"reviewed",
				summary,
				occurredAt,
				Map.of()
		);
	}

	private KnowledgePromotionPlanRecord plan(Instant occurredAt) {
		return new KnowledgePromotionPlanRecord(
				"promotion-plan-1",
				"candidate-1",
				"incident-1",
				KnowledgePromotionPlanStatus.PLAN_CREATED,
				"planner-a",
				"plan summary",
				List.of(new KnowledgePromotionPlanTarget(
						KnowledgePromotionTargetType.RUNBOOK,
						"runbooks/payment.md",
						"update",
						List.of(),
						List.of()
				)),
				List.of(),
				List.of(),
				occurredAt,
				Map.of()
		);
	}

	private KnowledgeUpdateApplicationRecord update(Instant occurredAt) {
		return new KnowledgeUpdateApplicationRecord(
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
				List.of("link-check", "customer token validation"),
				occurredAt,
				Map.of("secret", "hidden")
		);
	}
}
