package com.fintech.sre.agent.persistence.r2dbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.sre.agent.recommendation.execution.result.HumanExecutionResultRecord;
import com.fintech.sre.agent.recommendation.execution.result.HumanExecutionStatus;

class HumanExecutionResultEntityMapperTest {

	@Test
	void shouldRoundTripHumanExecutionResultAndFilterSensitiveMetadata() {
		HumanExecutionResultEntityMapper mapper =
				new HumanExecutionResultEntityMapper(new ObjectMapper());

		HumanExecutionResultRecord record = new HumanExecutionResultRecord(
				"result-1",
				"plan-1",
				"rec-1",
				"incident-1",
				HumanExecutionStatus.EXECUTED,
				"operator-a",
				"manual action completed",
				Instant.parse("2026-05-09T00:00:00Z"),
				Instant.parse("2026-05-09T00:10:00Z"),
				Instant.parse("2026-05-09T00:11:00Z"),
				Map.of(
						"reviewGroup", "sre",
						"paymentPayload", "must-not-store"
				)
		);

		HumanExecutionResultEntity entity = mapper.toEntity(record);
		HumanExecutionResultRecord restored = mapper.toDomain(entity);

		assertThat(entity.getMetadataJson()).contains("reviewGroup");
		assertThat(entity.getMetadataJson()).doesNotContain("paymentPayload");
		assertThat(restored.metadata())
				.containsEntry("reviewGroup", "sre")
				.doesNotContainKey("paymentPayload");
		assertThat(restored.summary()).isEqualTo("manual action completed");
	}
}
