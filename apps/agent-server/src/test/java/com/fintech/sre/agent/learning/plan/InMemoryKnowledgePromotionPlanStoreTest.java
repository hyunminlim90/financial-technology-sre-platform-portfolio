package com.fintech.sre.agent.learning.plan;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class InMemoryKnowledgePromotionPlanStoreTest {

	@Test
	void shouldSaveAndFindPlans() {
		InMemoryKnowledgePromotionPlanStore store = new InMemoryKnowledgePromotionPlanStore();

		KnowledgePromotionPlanRecord record = new KnowledgePromotionPlanRecord(
				"plan-1",
				"candidate-1",
				"incident-1",
				KnowledgePromotionPlanStatus.PLAN_CREATED,
				"operator-a",
				"Plan runbook update.",
				List.of(new KnowledgePromotionPlanTarget(
						KnowledgePromotionTargetType.RUNBOOK,
						"runbooks/payment/payment-api-runbook.md",
						"Update rollback section.",
						List.of("Add explicit rollback command."),
						List.of("Confirm rollback step exists.")
				)),
				List.of("Human must edit knowledge files."),
				List.of(),
				Instant.now(),
				Map.of()
		);

		store.save(record).block();

		assertThat(store.findByLearningCandidateId("candidate-1").collectList().block())
				.hasSize(1);
		assertThat(store.findByIncidentId("incident-1").collectList().block())
				.hasSize(1);
	}
}
