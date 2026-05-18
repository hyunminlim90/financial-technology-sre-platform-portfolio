package com.fintech.sre.agent.persistence.r2dbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalRecord;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalStatus;

class RecommendationApprovalRecordEntityMapperTest {

	@Test
	void shouldRoundTripApprovalRecordAndFilterSensitiveMetadata() {
		RecommendationApprovalRecordEntityMapper mapper =
				new RecommendationApprovalRecordEntityMapper(new ObjectMapper());

		RecommendationApprovalRecord record = new RecommendationApprovalRecord(
				"approval-1",
				"rec-1",
				"incident-1",
				RecommendationApprovalStatus.APPROVED,
				"operator-a",
				"approved",
				Instant.parse("2026-05-09T00:00:00Z"),
				Map.of(
						"reviewGroup", "sre",
						"paymentPayload", "must-not-store"
				)
		);

		RecommendationApprovalRecordEntity entity = mapper.toEntity(record);
		RecommendationApprovalRecord restored = mapper.toDomain(entity);

		assertThat(entity.getMetadataJson()).contains("reviewGroup");
		assertThat(entity.getMetadataJson()).doesNotContain("paymentPayload");
		assertThat(restored.metadata())
				.containsEntry("reviewGroup", "sre")
				.doesNotContainKey("paymentPayload");
	}
}
