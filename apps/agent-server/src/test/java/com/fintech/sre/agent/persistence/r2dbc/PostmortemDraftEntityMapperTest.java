package com.fintech.sre.agent.persistence.r2dbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.sre.agent.postmortem.draft.PostmortemDraftRecord;
import com.fintech.sre.agent.postmortem.draft.PostmortemDraftStatus;

class PostmortemDraftEntityMapperTest {

	@Test
	void shouldRoundTripPostmortemDraftAndFilterSensitiveMetadata() {
		PostmortemDraftEntityMapper mapper =
				new PostmortemDraftEntityMapper(new ObjectMapper());

		PostmortemDraftRecord record = new PostmortemDraftRecord(
				"draft-1",
				"incident-1",
				PostmortemDraftStatus.HUMAN_REVIEW_REQUIRED,
				"operator-a",
				"This draft does not assert root cause certainty.",
				List.of("00:00 alert", "00:10 mitigation"),
				List.of("verify rollback steps"),
				List.of("manual restart completed"),
				List.of("verification passed"),
				List.of("confirm traffic regression pattern"),
				List.of("update runbook"),
				List.of("What supporting evidence is still missing?"),
				Instant.parse("2026-05-09T02:00:00Z"),
				Map.of(
						"reviewGroup", "sre",
						"promptTemplate", "must-not-store"
				)
		);

		PostmortemDraftEntity entity = mapper.toEntity(record);
		PostmortemDraftRecord restored = mapper.toDomain(entity);

		assertThat(entity.getMetadataJson()).contains("reviewGroup");
		assertThat(entity.getMetadataJson()).doesNotContain("promptTemplate");
		assertThat(restored.timeline()).containsExactly("00:00 alert", "00:10 mitigation");
		assertThat(restored.learningCandidates()).containsExactly("update runbook");
		assertThat(restored.metadata())
				.containsEntry("reviewGroup", "sre")
				.doesNotContainKey("promptTemplate");
		assertThat(restored.summary())
				.contains("does not assert root cause certainty");
	}
}
