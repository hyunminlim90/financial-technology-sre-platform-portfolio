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
import com.fintech.sre.agent.learning.candidate.LearningCandidateStore;
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

class GovernanceLearningDetailServiceTest {

	@Test
	void shouldBuildLearningDetailAggregate() {
		Instant base = Instant.parse("2026-05-10T00:00:00Z");
		InMemoryLearningCandidateStore learningCandidateStore = new InMemoryLearningCandidateStore();
		InMemoryKnowledgePromotionReviewStore reviewStore = new InMemoryKnowledgePromotionReviewStore();
		InMemoryKnowledgePromotionPlanStore planStore = new InMemoryKnowledgePromotionPlanStore();
		InMemoryKnowledgeUpdateApplicationStore updateStore = new InMemoryKnowledgeUpdateApplicationStore();
		SimpleMeterRegistry registry = new SimpleMeterRegistry();

		learningCandidateStore.save(candidate(base.plusSeconds(10))).block();
		reviewStore.save(review(base.plusSeconds(20))).block();
		planStore.save(plan(base.plusSeconds(30))).block();
		updateStore.save(update(base.plusSeconds(40))).block();

		GovernanceLearningDetailService service = service(
				learningCandidateStore,
				reviewStore,
				planStore,
				updateStore,
				resilience(),
				registry
		);

		GovernanceLearningDetailResponse response =
				service.findByLearningCandidateId("candidate-1").block();

		assertThat(response.learningCandidateId()).isEqualTo("candidate-1");
		assertThat(response.degradation()).isEqualTo(GovernanceDetailDegradation.none());
		assertThat(response.learningCandidate()).isNotNull();
		assertThat(response.promotionReviews()).hasSize(1);
		assertThat(response.promotionPlans()).hasSize(1);
		assertThat(response.knowledgeUpdates()).hasSize(1);
		assertThat(response.timeline()).isSortedAccordingTo(
				java.util.Comparator.comparing(GovernanceDetailTimelineItem::occurredAt)
		);
		assertThat(response.promotionReviews().get(0).summary()).isEqualTo("[redacted]");
		assertThat(registry.find(GovernanceDetailMetricName.QUERY_TOTAL)
				.tag("detailType", "learningCandidate")
				.tag("result", "success")
				.counter()
				.count()).isEqualTo(1.0);
		assertThat(registry.find(GovernanceDetailMetricName.DEGRADED_TOTAL)
				.tag("detailType", "learningCandidate")
				.counter()).isNull();
	}

	@Test
	void shouldReturnNotFoundWhenLearningCandidateDoesNotExist() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceLearningDetailService service = service(
				new InMemoryLearningCandidateStore(),
				new InMemoryKnowledgePromotionReviewStore(),
				new InMemoryKnowledgePromotionPlanStore(),
				new InMemoryKnowledgeUpdateApplicationStore(),
				resilience(),
				registry
		);

		assertThatThrownBy(() -> service.findByLearningCandidateId("missing").block())
				.isInstanceOf(ResponseStatusException.class);
		assertThat(registry.find(GovernanceDetailMetricName.QUERY_NOT_FOUND)
				.tag("detailType", "learningCandidate")
				.tag("result", "not_found")
				.counter()
				.count()).isEqualTo(1.0);
	}

	@Test
	void shouldReturnPartialDegradedLearningDetailWhenChildComponentFails() {
		Instant base = Instant.parse("2026-05-10T00:00:00Z");
		InMemoryLearningCandidateStore learningCandidateStore = new InMemoryLearningCandidateStore();
		InMemoryKnowledgePromotionPlanStore planStore = new InMemoryKnowledgePromotionPlanStore();
		InMemoryKnowledgeUpdateApplicationStore updateStore = new InMemoryKnowledgeUpdateApplicationStore();
		SimpleMeterRegistry registry = new SimpleMeterRegistry();

		learningCandidateStore.save(candidate(base.plusSeconds(10))).block();
		planStore.save(plan(base.plusSeconds(30))).block();
		updateStore.save(update(base.plusSeconds(40))).block();

		GovernanceLearningDetailService service = service(
				learningCandidateStore,
				failingReviewStore(),
				planStore,
				updateStore,
				resilienceEnabled(),
				registry
		);

		GovernanceLearningDetailResponse response =
				service.findByLearningCandidateId("candidate-1").block();

		assertThat(response.degradation().degraded()).isTrue();
		assertThat(response.degradation().failedComponents()).containsExactly("promotionReviews");
		assertThat(response.promotionReviews()).isEmpty();
		assertThat(response.timeline().stream()
				.map(GovernanceDetailTimelineItem::type)
				.toList()).doesNotContain("PROMOTION_REVIEWED");
		assertThat(registry.find(GovernanceDetailMetricName.DEGRADED_TOTAL)
				.tag("detailType", "learningCandidate")
				.tag("result", "degraded")
				.tag("reason", "component_query_failed")
				.tag("component", "promotionReviews")
				.counter()
				.count()).isEqualTo(1.0);
	}

	@Test
	void shouldPropagateLearningChildFailureWhenPartialResponseDisabled() {
		Instant base = Instant.parse("2026-05-10T00:00:00Z");
		InMemoryLearningCandidateStore learningCandidateStore = new InMemoryLearningCandidateStore();
		learningCandidateStore.save(candidate(base.plusSeconds(10))).block();

		GovernanceLearningDetailService service = service(
				learningCandidateStore,
				failingReviewStore(),
				new InMemoryKnowledgePromotionPlanStore(),
				new InMemoryKnowledgeUpdateApplicationStore(),
				partialDisabled(),
				new SimpleMeterRegistry()
		);

		assertThatThrownBy(() -> service.findByLearningCandidateId("candidate-1").block())
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void shouldPropagateLearningChildFailureWhenFailOpenDisabled() {
		Instant base = Instant.parse("2026-05-10T00:00:00Z");
		InMemoryLearningCandidateStore learningCandidateStore = new InMemoryLearningCandidateStore();
		learningCandidateStore.save(candidate(base.plusSeconds(10))).block();

		GovernanceLearningDetailService service = service(
				learningCandidateStore,
				failingReviewStore(),
				new InMemoryKnowledgePromotionPlanStore(),
				new InMemoryKnowledgeUpdateApplicationStore(),
				failOpenDisabled(),
				new SimpleMeterRegistry()
		);

		assertThatThrownBy(() -> service.findByLearningCandidateId("candidate-1").block())
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void shouldRecordFailureMetricWhenLearningDetailQueryFails() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GovernanceLearningDetailService service = service(
				failingLearningCandidateStore(),
				new InMemoryKnowledgePromotionReviewStore(),
				new InMemoryKnowledgePromotionPlanStore(),
				new InMemoryKnowledgeUpdateApplicationStore(),
				resilience(),
				registry
		);

		assertThatThrownBy(() -> service.findByLearningCandidateId("candidate-1").block())
				.isInstanceOf(IllegalStateException.class);
		assertThat(registry.find(GovernanceDetailMetricName.QUERY_TOTAL)
				.tag("detailType", "learningCandidate")
				.tag("result", "failure")
				.counter()
				.count()).isEqualTo(1.0);
	}

	private GovernanceLearningDetailService service(
			LearningCandidateStore learningCandidateStore,
			KnowledgePromotionReviewStore reviewStore,
			KnowledgePromotionPlanStore planStore,
			KnowledgeUpdateApplicationStore updateStore,
			GovernanceDetailResilienceProperties properties,
			SimpleMeterRegistry registry
	) {
		return new GovernanceLearningDetailService(
				learningCandidateStore,
				reviewStore,
				planStore,
				updateStore,
				new GovernanceDetailTimelineBuilder(new GovernanceDetailSanitizer()),
				new GovernanceDetailSanitizer(),
				new GovernanceDetailComponentLoader(properties),
				recorder(registry)
		);
	}

	private LearningCandidateStore failingLearningCandidateStore() {
		return new LearningCandidateStore() {
			@Override
			public Mono<LearningCandidateRecord> save(LearningCandidateRecord record) {
				return Mono.just(record);
			}

			@Override
			public Mono<LearningCandidateRecord> findById(String learningCandidateId) {
				return Mono.error(new IllegalStateException("boom"));
			}

			@Override
			public Flux<LearningCandidateRecord> findByIncidentId(String incidentId) {
				return Flux.empty();
			}

			@Override
			public Flux<LearningCandidateRecord> findRecent(int limit) {
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

	private KnowledgePromotionReviewRecord review(Instant occurredAt) {
		return new KnowledgePromotionReviewRecord(
				"promotion-review-1",
				"candidate-1",
				"incident-1",
				KnowledgePromotionReviewStatus.APPROVED_FOR_PROMOTION,
				"reviewer-a",
				"reviewed",
				"token found in draft review",
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
				List.of("link-check"),
				occurredAt,
				Map.of()
		);
	}
}
