package com.fintech.sre.agent.learning.promotion;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;

class InMemoryKnowledgePromotionReviewStoreTest {

	@Test
	void shouldSaveAndQueryReviewHistory() {
		InMemoryKnowledgePromotionReviewStore store = new InMemoryKnowledgePromotionReviewStore();

		KnowledgePromotionReviewRecord first = new KnowledgePromotionReviewRecord(
				"review-1",
				"candidate-1",
				"incident-1",
				KnowledgePromotionReviewStatus.NEEDS_REVISION,
				"operator-a",
				"needs edits",
				"Please narrow proposed change scope.",
				Instant.now().minusSeconds(60),
				Map.of()
		);

		KnowledgePromotionReviewRecord second = new KnowledgePromotionReviewRecord(
				"review-2",
				"candidate-1",
				"incident-1",
				KnowledgePromotionReviewStatus.APPROVED_FOR_PROMOTION,
				"operator-b",
				"ready",
				"Eligible for promotion planning.",
				Instant.now(),
				Map.of()
		);

		store.save(first).block();
		store.save(second).block();

		assertThat(store.findLatestByLearningCandidateId("candidate-1").block().status())
				.isEqualTo(KnowledgePromotionReviewStatus.APPROVED_FOR_PROMOTION);
		assertThat(store.findByIncidentId("incident-1").collectList().block())
				.hasSize(2);
	}
}
