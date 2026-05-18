package com.fintech.sre.agent.postmortem.draft;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class InMemoryPostmortemDraftStoreTest {

	@Test
	void shouldSaveAndFindByIncidentId() {
		InMemoryPostmortemDraftStore store = new InMemoryPostmortemDraftStore();

		PostmortemDraftRecord record = new PostmortemDraftRecord(
				"draft-1",
				"incident-1",
				PostmortemDraftStatus.HUMAN_REVIEW_REQUIRED,
				"operator-a",
				"summary",
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				Instant.now(),
				Map.of()
		);

		store.save(record).block();

		assertThat(store.findByIncidentId("incident-1").collectList().block())
				.hasSize(1);
	}
}
