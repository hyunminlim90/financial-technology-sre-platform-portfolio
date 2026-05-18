package com.fintech.sre.agent.learning.plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.observability.metrics.MetricsRecorderTestSupport;
import com.fintech.sre.agent.learning.candidate.InMemoryLearningCandidateStore;
import com.fintech.sre.agent.learning.candidate.LearningCandidateRecord;
import com.fintech.sre.agent.learning.candidate.LearningCandidateStatus;
import com.fintech.sre.agent.learning.candidate.LearningCandidateType;
import com.fintech.sre.agent.learning.promotion.InMemoryKnowledgePromotionReviewStore;
import com.fintech.sre.agent.learning.promotion.KnowledgePromotionReviewRecord;
import com.fintech.sre.agent.learning.promotion.KnowledgePromotionReviewStatus;

class KnowledgePromotionPlanServiceTest {

	@Test
	void shouldCreatePlanForApprovedPromotionReview() {
		InMemoryLearningCandidateStore candidateStore = new InMemoryLearningCandidateStore();
		InMemoryKnowledgePromotionReviewStore reviewStore = new InMemoryKnowledgePromotionReviewStore();
		InMemoryKnowledgePromotionPlanStore planStore = new InMemoryKnowledgePromotionPlanStore();

		candidateStore.save(candidate()).block();
		reviewStore.save(review(KnowledgePromotionReviewStatus.APPROVED_FOR_PROMOTION)).block();

		KnowledgePromotionPlanService service = new KnowledgePromotionPlanService(
				candidateStore,
				reviewStore,
				planStore,
				new KnowledgePromotionTargetPlanner(),
				new KnowledgePromotionPlanIdGenerator(),
				MetricsRecorderTestSupport.learningMetricsRecorder()
		);

		KnowledgePromotionPlanResponse response = service.createPlan(
				"candidate-1",
				new KnowledgePromotionPlanRequest(
						"operator-a",
						"Plan the manual runbook update.",
						Map.of(
								"team", "sre",
								"paymentPayload", "must-not-store"
						)
				)
		).block();

		assertThat(response.status()).isEqualTo(KnowledgePromotionPlanStatus.PLAN_CREATED);
		assertThat(response.targets()).isNotEmpty();
		assertThat(response.targets().get(0).recommendedPath()).contains("runbooks");

		KnowledgePromotionPlanRecord stored = planStore.findById(response.promotionPlanId()).block();
		assertThat(stored.metadata()).containsKey("team");
		assertThat(stored.metadata()).doesNotContainKey("paymentPayload");
	}

	@Test
	void shouldRejectWhenReviewNotApprovedForPromotion() {
		InMemoryLearningCandidateStore candidateStore = new InMemoryLearningCandidateStore();
		InMemoryKnowledgePromotionReviewStore reviewStore = new InMemoryKnowledgePromotionReviewStore();

		candidateStore.save(candidate()).block();
		reviewStore.save(review(KnowledgePromotionReviewStatus.NEEDS_REVISION)).block();

		KnowledgePromotionPlanService service = new KnowledgePromotionPlanService(
				candidateStore,
				reviewStore,
				new InMemoryKnowledgePromotionPlanStore(),
				new KnowledgePromotionTargetPlanner(),
				new KnowledgePromotionPlanIdGenerator(),
				MetricsRecorderTestSupport.learningMetricsRecorder()
		);

		assertThatThrownBy(() -> service.createPlan(
				"candidate-1",
				new KnowledgePromotionPlanRequest(
						"operator-a",
						"summary",
						Map.of()
				)
		).block())
				.isInstanceOf(KnowledgePromotionPlanRejectedException.class)
				.hasMessage("Latest promotion review must be APPROVED_FOR_PROMOTION.");
	}

	private LearningCandidateRecord candidate() {
		return new LearningCandidateRecord(
				"candidate-1",
				"incident-1",
				"draft-1",
				"review-1",
				LearningCandidateType.RUNBOOK_UPDATE,
				LearningCandidateStatus.REVIEW_REQUIRED,
				"operator-a",
				"Promote approved runbook change.",
				List.of("Add verification checklist."),
				Instant.now(),
				Map.of(
						"domain", "payment",
						"service", "payment-api"
				)
		);
	}

	private KnowledgePromotionReviewRecord review(KnowledgePromotionReviewStatus status) {
		return new KnowledgePromotionReviewRecord(
				"promotion-review-1",
				"candidate-1",
				"incident-1",
				status,
				"operator-a",
				"reviewed",
				"Ready for promotion planning.",
				Instant.now(),
				Map.of()
		);
	}
}
