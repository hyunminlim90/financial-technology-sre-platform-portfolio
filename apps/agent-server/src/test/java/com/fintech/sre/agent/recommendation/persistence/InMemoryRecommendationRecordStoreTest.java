package com.fintech.sre.agent.recommendation.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class InMemoryRecommendationRecordStoreTest {

	@Test
	void shouldSaveAndFindByIncidentId() {
		InMemoryRecommendationRecordStore store = new InMemoryRecommendationRecordStore();

		RecommendationRecord record = new RecommendationRecord(
				"rec-1",
				"incident-1",
				"audit-1",
				"PROMETHEUS_ALERTMANAGER",
				"payment-api",
				"payment",
				"CRITICAL",
				"firing",
				Instant.now(),
				1,
				0,
				"ALLOW",
				"PASS",
				List.of("RATE_LIMIT"),
				List.of(),
				Map.of("alertName", "HighP99Latency")
		);

		store.save(record).block();

		assertThat(store.findByIncidentId("incident-1").collectList().block())
				.hasSize(1);
	}
}
