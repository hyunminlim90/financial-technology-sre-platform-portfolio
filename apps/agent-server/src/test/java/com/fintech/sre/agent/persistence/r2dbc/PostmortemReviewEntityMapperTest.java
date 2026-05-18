package com.fintech.sre.agent.persistence.r2dbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.sre.agent.postmortem.review.PostmortemReviewRecord;
import com.fintech.sre.agent.postmortem.review.PostmortemReviewStatus;

class PostmortemReviewEntityMapperTest {

	@Test
	void shouldRoundTripPostmortemReviewAndFilterSensitiveMetadata() {
		PostmortemReviewEntityMapper mapper =
				new PostmortemReviewEntityMapper(new ObjectMapper());

		PostmortemReviewRecord record = new PostmortemReviewRecord(
				"review-1",
				"draft-1",
				"incident-1",
				PostmortemReviewStatus.NEEDS_REVISION,
				"reviewer-a",
				"More evidence is needed.",
				"This draft does not yet establish sufficient causal support.",
				Instant.parse("2026-05-09T04:00:00Z"),
				Map.of(
						"reviewGroup", "sre",
						"customerToken", "must-not-store"
				)
		);

		PostmortemReviewEntity entity = mapper.toEntity(record);
		PostmortemReviewRecord restored = mapper.toDomain(entity);

		assertThat(entity.getMetadataJson()).contains("reviewGroup");
		assertThat(entity.getMetadataJson()).doesNotContain("customerToken");
		assertThat(restored.metadata())
				.containsEntry("reviewGroup", "sre")
				.doesNotContainKey("customerToken");
		assertThat(restored.status()).isEqualTo(PostmortemReviewStatus.NEEDS_REVISION);
		assertThat(restored.reviewSummary()).contains("causal support");
	}
}
