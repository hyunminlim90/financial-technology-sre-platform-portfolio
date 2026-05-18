package com.fintech.sre.agent.learning.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class InMemoryKnowledgeUpdateApplicationStoreTest {

	@Test
	void shouldSaveAndFindByIncidentIdAndLearningCandidateId() {
		InMemoryKnowledgeUpdateApplicationStore store =
				new InMemoryKnowledgeUpdateApplicationStore();

		KnowledgeUpdateApplicationRecord record =
				new KnowledgeUpdateApplicationRecord(
						"knowledge-update-1",
						"incident-1",
						"candidate-1",
						"plan-1",
						"RUNBOOK",
						KnowledgeUpdateLayer.PRIMARY_OPERATIONAL_KNOWLEDGE,
						"runbooks/payment/payment-api-runbook.md",
						KnowledgeUpdateChangeType.UPDATED,
						"portfolio-repo",
						"main",
						"a1b2c3d4",
						"PR-101",
						"operator-a",
						"reviewer-a",
						"approver-a",
						List.of("Runbook syntax checked."),
						Instant.now(),
						Map.of()
				);

		store.save(record).block();

		assertThat(store.findByIncidentId("incident-1").collectList().block())
				.hasSize(1);
		assertThat(store.findByLearningCandidateId("candidate-1").collectList().block())
				.hasSize(1);
	}
}
