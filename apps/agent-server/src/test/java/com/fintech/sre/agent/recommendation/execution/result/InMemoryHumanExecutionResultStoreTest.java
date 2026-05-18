package com.fintech.sre.agent.recommendation.execution.result;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;

class InMemoryHumanExecutionResultStoreTest {

	@Test
	void shouldSaveAndFindByExecutionPlanId() {
		InMemoryHumanExecutionResultStore store =
				new InMemoryHumanExecutionResultStore();

		HumanExecutionResultRecord record =
				new HumanExecutionResultRecord(
						"result-1",
						"plan-1",
						"rec-1",
						"incident-1",
						HumanExecutionStatus.EXECUTED,
						"operator-a",
						"manual action applied",
						Instant.now().minusSeconds(60),
						Instant.now(),
						Instant.now(),
						Map.of()
				);

		store.save(record).block();

		assertThat(store.findByExecutionPlanId("plan-1").collectList().block())
				.hasSize(1);
	}
}
