package com.fintech.sre.agent.persistence.r2dbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.sre.agent.incident.lifecycle.IncidentLifecycleRecord;
import com.fintech.sre.agent.incident.lifecycle.IncidentStatus;
import com.fintech.sre.agent.incident.lifecycle.IncidentTransitionReason;

class IncidentLifecycleEntityMapperTest {

	@Test
	void shouldRoundTripIncidentLifecycleAndFilterSensitiveMetadata() {
		IncidentLifecycleEntityMapper mapper =
				new IncidentLifecycleEntityMapper(new ObjectMapper());

		IncidentLifecycleRecord record = new IncidentLifecycleRecord(
				"lifecycle-1",
				"incident-1",
				IncidentStatus.MITIGATING,
				IncidentStatus.STABILIZING,
				IncidentTransitionReason.STABILIZATION_WINDOW_STARTED,
				"operator-a",
				"manual stabilization confirmed",
				Instant.parse("2026-05-09T01:15:00Z"),
				Map.of(
						"reviewGroup", "sre",
						"rawLog", "must-not-store"
				)
		);

		IncidentLifecycleEntity entity = mapper.toEntity(record);
		IncidentLifecycleRecord restored = mapper.toDomain(entity);

		assertThat(entity.getMetadataJson()).contains("reviewGroup");
		assertThat(entity.getMetadataJson()).doesNotContain("rawLog");
		assertThat(restored.metadata())
				.containsEntry("reviewGroup", "sre")
				.doesNotContainKey("rawLog");
		assertThat(restored.previousStatus()).isEqualTo(IncidentStatus.MITIGATING);
		assertThat(restored.currentStatus()).isEqualTo(IncidentStatus.STABILIZING);
		assertThat(restored.transitionReason())
				.isEqualTo(IncidentTransitionReason.STABILIZATION_WINDOW_STARTED);
	}
}
