package com.fintech.sre.agent.recommendation.approval;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;

class InMemoryRecommendationApprovalStoreTest {

	@Test
	void shouldFindLatestApprovalByRecommendationRecordId() {
		InMemoryRecommendationApprovalStore store = new InMemoryRecommendationApprovalStore();

		RecommendationApprovalRecord first = new RecommendationApprovalRecord(
				"approval-1",
				"rec-1",
				"incident-1",
				RecommendationApprovalStatus.REJECTED,
				"operator-a",
				"need more evidence",
				Instant.now().minusSeconds(60),
				Map.of()
		);

		RecommendationApprovalRecord second = new RecommendationApprovalRecord(
				"approval-2",
				"rec-1",
				"incident-1",
				RecommendationApprovalStatus.APPROVED,
				"operator-b",
				"evidence confirmed",
				Instant.now(),
				Map.of()
		);

		store.save(first).block();
		store.save(second).block();

		RecommendationApprovalRecord latest =
				store.findLatestByRecommendationRecordId("rec-1").block();

		assertThat(latest.status()).isEqualTo(RecommendationApprovalStatus.APPROVED);
	}
}
