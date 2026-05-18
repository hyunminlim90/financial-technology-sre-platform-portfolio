package com.fintech.sre.agent.recommendation.verification;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;

class InMemoryVerificationResultStoreTest {

	@Test
	void shouldSaveAndFindByExecutionResultId() {
		InMemoryVerificationResultStore store =
				new InMemoryVerificationResultStore();

		VerificationResultRecord record =
				new VerificationResultRecord(
						"verification-1",
						"execution-result-1",
						"execution-plan-1",
						"recommendation-1",
						"incident-1",
						VerificationStatus.VERIFIED,
						"operator-a",
						"Latency normalized",
						Instant.now(),
						Map.of()
				);

		store.save(record).block();

		assertThat(
				store.findByExecutionResultId("execution-result-1")
						.collectList()
						.block()
		).hasSize(1);
	}
}
