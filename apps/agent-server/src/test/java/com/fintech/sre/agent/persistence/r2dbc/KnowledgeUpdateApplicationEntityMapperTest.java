package com.fintech.sre.agent.persistence.r2dbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.sre.agent.learning.application.KnowledgeUpdateApplicationRecord;
import com.fintech.sre.agent.learning.application.KnowledgeUpdateChangeType;
import com.fintech.sre.agent.learning.application.KnowledgeUpdateLayer;

class KnowledgeUpdateApplicationEntityMapperTest {

	@Test
	void shouldRoundTripKnowledgeUpdateApplicationAndFilterSensitiveFields() {
		KnowledgeUpdateApplicationEntityMapper mapper =
				new KnowledgeUpdateApplicationEntityMapper(new ObjectMapper());

		KnowledgeUpdateApplicationRecord record = new KnowledgeUpdateApplicationRecord(
				"knowledge-update-1",
				"incident-1",
				"candidate-1",
				"promotion-plan-1",
				"RUNBOOK",
				KnowledgeUpdateLayer.PRIMARY_OPERATIONAL_KNOWLEDGE,
				"runbooks/payments/payment-api-runbook.md",
				KnowledgeUpdateChangeType.UPDATED,
				"portfolio-repo",
				"main",
				"a1b2c3d4",
				"PR-100",
				"operator-a",
				"reviewer-a",
				"approver-a",
				List.of(
						"rollback verification completed",
						"customer payload verified"
				),
				Instant.parse("2026-05-09T12:00:00Z"),
				Map.of(
						"reviewGroup", "sre",
						"promptPayload", "must-not-store"
				)
		);

		KnowledgeUpdateApplicationEntity entity = mapper.toEntity(record);
		KnowledgeUpdateApplicationRecord restored = mapper.toDomain(entity);

		assertThat(entity.getMetadataJson()).contains("reviewGroup");
		assertThat(entity.getMetadataJson()).doesNotContain("promptPayload");
		assertThat(entity.getValidationChecksJson())
				.contains("rollback verification completed")
				.doesNotContain("customer payload verified");
		assertThat(restored.validationChecks())
				.containsExactly("rollback verification completed");
		assertThat(restored.metadata())
				.containsEntry("reviewGroup", "sre")
				.doesNotContainKey("promptPayload");
		assertThat(restored.gitCommitSha()).isEqualTo("a1b2c3d4");
	}
}
