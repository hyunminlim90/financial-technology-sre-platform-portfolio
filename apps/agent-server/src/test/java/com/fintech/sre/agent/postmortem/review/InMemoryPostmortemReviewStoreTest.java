package com.fintech.sre.agent.postmortem.review;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;

class InMemoryPostmortemReviewStoreTest {

	@Test
	void shouldSaveAndQueryReviewHistory() {
		InMemoryPostmortemReviewStore store = new InMemoryPostmortemReviewStore();

		PostmortemReviewRecord first = new PostmortemReviewRecord(
				"review-1",
				"draft-1",
				"incident-1",
				PostmortemReviewStatus.NEEDS_REVISION,
				"operator-a",
				"needs more detail",
				"Please clarify contributing factors.",
				Instant.now().minusSeconds(60),
				Map.of()
		);

		PostmortemReviewRecord second = new PostmortemReviewRecord(
				"review-2",
				"draft-1",
				"incident-1",
				PostmortemReviewStatus.APPROVED,
				"operator-b",
				"review complete",
				"Draft is acceptable for learning review.",
				Instant.now(),
				Map.of()
		);

		store.save(first).block();
		store.save(second).block();

		assertThat(store.findLatestByDraftId("draft-1").block().status())
				.isEqualTo(PostmortemReviewStatus.APPROVED);
		assertThat(store.findByIncidentId("incident-1").collectList().block())
				.hasSize(2);
	}
}
