package com.fintech.sre.agent.persistence.r2dbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.sre.agent.learning.promotion.KnowledgePromotionReviewRecord;
import com.fintech.sre.agent.learning.promotion.KnowledgePromotionReviewStatus;

class KnowledgePromotionReviewEntityMapperTest {

	@Test
	void shouldRoundTripKnowledgePromotionReviewAndFilterSensitiveMetadata() {
		KnowledgePromotionReviewEntityMapper mapper =
				new KnowledgePromotionReviewEntityMapper(new ObjectMapper());

		KnowledgePromotionReviewRecord record = new KnowledgePromotionReviewRecord(
				"promotion-review-1",
				"candidate-1",
				"incident-1",
				KnowledgePromotionReviewStatus.APPROVED_FOR_PROMOTION,
				"reviewer-a",
				"Ready for human promotion planning.",
				"Eligible for planning but not yet a file update.",
				Instant.parse("2026-05-09T08:00:00Z"),
				Map.of(
						"reviewGroup", "sre",
						"secretToken", "must-not-store"
				)
		);

		KnowledgePromotionReviewEntity entity = mapper.toEntity(record);
		KnowledgePromotionReviewRecord restored = mapper.toDomain(entity);

		assertThat(entity.getMetadataJson()).contains("reviewGroup");
		assertThat(entity.getMetadataJson()).doesNotContain("secretToken");
		assertThat(restored.metadata())
				.containsEntry("reviewGroup", "sre")
				.doesNotContainKey("secretToken");
		assertThat(restored.status())
				.isEqualTo(KnowledgePromotionReviewStatus.APPROVED_FOR_PROMOTION);
		assertThat(restored.reviewSummary()).contains("not yet a file update");
	}
}
