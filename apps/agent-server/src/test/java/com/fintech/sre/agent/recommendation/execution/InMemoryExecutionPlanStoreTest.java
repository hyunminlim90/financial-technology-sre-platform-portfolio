package com.fintech.sre.agent.recommendation.execution;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class InMemoryExecutionPlanStoreTest {

	@Test
	void shouldSaveAndFindByRecommendationRecordId() {
		InMemoryExecutionPlanStore store = new InMemoryExecutionPlanStore();

		RecommendationExecutionPlan plan = new RecommendationExecutionPlan(
				"plan-1",
				"rec-1",
				"incident-1",
				ExecutionPlanStatus.DRY_RUN_PLAN_CREATED,
				false,
				true,
				"operator-a",
				"dry-run",
				Instant.now(),
				List.of(),
				List.of()
		);

		store.save(plan).block();

		assertThat(store.findByRecommendationRecordId("rec-1").collectList().block())
				.hasSize(1);
	}
}
