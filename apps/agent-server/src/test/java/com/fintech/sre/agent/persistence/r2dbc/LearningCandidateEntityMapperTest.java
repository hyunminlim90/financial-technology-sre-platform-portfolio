package com.fintech.sre.agent.persistence.r2dbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.sre.agent.learning.candidate.LearningCandidateRecord;
import com.fintech.sre.agent.learning.candidate.LearningCandidateStatus;
import com.fintech.sre.agent.learning.candidate.LearningCandidateType;

class LearningCandidateEntityMapperTest {

	@Test
	void shouldRoundTripLearningCandidateAndFilterSensitiveFields() {
		LearningCandidateEntityMapper mapper =
				new LearningCandidateEntityMapper(new ObjectMapper());

		LearningCandidateRecord record = new LearningCandidateRecord(
				"candidate-1",
				"incident-1",
				"draft-1",
				"review-1",
				LearningCandidateType.RUNBOOK_UPDATE,
				LearningCandidateStatus.REVIEW_REQUIRED,
				"operator-a",
				"Promote a runbook update candidate.",
				List.of(
						"Add rollback verification step.",
						"customer token should never appear"
				),
				Instant.parse("2026-05-09T06:00:00Z"),
				Map.of(
						"reviewGroup", "sre",
						"paymentPayload", "must-not-store"
				)
		);

		LearningCandidateEntity entity = mapper.toEntity(record);
		LearningCandidateRecord restored = mapper.toDomain(entity);

		assertThat(entity.getMetadataJson()).contains("reviewGroup");
		assertThat(entity.getMetadataJson()).doesNotContain("paymentPayload");
		assertThat(entity.getProposedChangesJson())
				.contains("Add rollback verification step.")
				.doesNotContain("customer token");
		assertThat(restored.proposedChanges())
				.containsExactly("Add rollback verification step.");
		assertThat(restored.metadata())
				.containsEntry("reviewGroup", "sre")
				.doesNotContainKey("paymentPayload");
	}
}
