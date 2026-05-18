package com.fintech.sre.agent.reanalysis;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;

class InMemoryReanalysisCandidateStoreTest {

	@Test
	void shouldSaveAndFindByIncidentId() {
		InMemoryReanalysisCandidateStore store =
				new InMemoryReanalysisCandidateStore();

		ReanalysisTriggerCandidate candidate =
				new ReanalysisTriggerCandidate(
						"candidate-1",
						"incident-1",
						"verification-1",
						"execution-1",
						ReanalysisTriggerReason.REGRESSION_DETECTED,
						ReanalysisCandidateStatus.PENDING_REANALYSIS,
						"operator-a",
						"Latency increased again",
						Instant.now(),
						Map.of()
				);

		store.save(candidate).block();

		assertThat(
				store.findByIncidentId("incident-1")
						.collectList()
						.block()
		).hasSize(1);
	}
}
