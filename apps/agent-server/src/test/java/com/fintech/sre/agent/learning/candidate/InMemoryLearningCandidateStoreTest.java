package com.fintech.sre.agent.learning.candidate;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class InMemoryLearningCandidateStoreTest {

	@Test
	void shouldSaveAndFindByIncidentId() {
		InMemoryLearningCandidateStore store =
				new InMemoryLearningCandidateStore();

		LearningCandidateRecord record =
				new LearningCandidateRecord(
						"candidate-1",
						"incident-1",
						"draft-1",
						"review-1",
						LearningCandidateType.RUNBOOK_UPDATE,
						LearningCandidateStatus.REVIEW_REQUIRED,
						"operator-a",
						"Promote runbook update candidate.",
						List.of("Add rollback check step."),
						Instant.now(),
						Map.of()
				);

		store.save(record).block();

		assertThat(store.findByIncidentId("incident-1").collectList().block())
				.hasSize(1);
	}
}
