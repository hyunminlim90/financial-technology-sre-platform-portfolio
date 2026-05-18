package com.fintech.sre.agent.persistence.r2dbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.sre.agent.recommendation.persistence.RecommendationRecord;

class RecommendationRecordEntityMapperTest {

	@Test
	void shouldRoundTripRecordAndFilterSensitiveMetadata() {
		RecommendationRecordEntityMapper mapper =
				new RecommendationRecordEntityMapper(new ObjectMapper());

		RecommendationRecord record = new RecommendationRecord(
				"rec-1",
				"incident-1",
				"audit-1",
				"PROMETHEUS_ALERTMANAGER",
				"payment-api",
				"payment",
				"CRITICAL",
				"firing",
				Instant.parse("2026-05-09T00:00:00Z"),
				1,
				0,
				"ALLOW",
				"PASS",
				List.of("RATE_LIMIT"),
				List.of("NO_SCENARIO"),
				Map.of(
						"domain", "payment",
						"paymentPayload", "must-not-store",
						"owner", "sre"
				)
		);

		RecommendationRecordEntity entity = mapper.toEntity(record);
		RecommendationRecord restored = mapper.toRecord(entity);

		assertThat(entity.getMetadataJson()).contains("domain");
		assertThat(entity.getMetadataJson()).doesNotContain("paymentPayload");
		assertThat(restored.metadata())
				.containsEntry("domain", "payment")
				.containsEntry("owner", "sre")
				.doesNotContainKey("paymentPayload");
		assertThat(restored.actionTypes()).containsExactly("RATE_LIMIT");
		assertThat(restored.blockedReasons()).containsExactly("NO_SCENARIO");
	}
}
