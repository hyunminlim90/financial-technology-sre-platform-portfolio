package com.fintech.sre.agent.persistence.r2dbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.sre.agent.recommendation.verification.VerificationResultRecord;
import com.fintech.sre.agent.recommendation.verification.VerificationStatus;

class VerificationResultEntityMapperTest {

	@Test
	void shouldRoundTripVerificationResultAndFilterSensitiveMetadata() {
		VerificationResultEntityMapper mapper =
				new VerificationResultEntityMapper(new ObjectMapper());

		VerificationResultRecord record = new VerificationResultRecord(
				"verification-1",
				"result-1",
				"plan-1",
				"rec-1",
				"incident-1",
				VerificationStatus.VERIFIED,
				"operator-a",
				"manual verification completed",
				Instant.parse("2026-05-09T00:15:00Z"),
				Map.of(
						"reviewGroup", "sre",
						"customerToken", "must-not-store"
				)
		);

		VerificationResultEntity entity = mapper.toEntity(record);
		VerificationResultRecord restored = mapper.toDomain(entity);

		assertThat(entity.getMetadataJson()).contains("reviewGroup");
		assertThat(entity.getMetadataJson()).doesNotContain("customerToken");
		assertThat(restored.metadata())
				.containsEntry("reviewGroup", "sre")
				.doesNotContainKey("customerToken");
		assertThat(restored.summary()).isEqualTo("manual verification completed");
	}
}
