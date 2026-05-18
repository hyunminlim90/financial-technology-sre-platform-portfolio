package com.fintech.sre.agent.governance.timeline.projection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class GovernanceTimelineProjectionRecordTest {

	@Test
	void shouldCreateProjectionRecord() {
		Instant occurredAt = Instant.parse("2026-05-17T00:00:00Z");
		Instant createdAt = Instant.parse("2026-05-17T00:00:05Z");
		GovernanceTimelineProjectionRecord record =
				new GovernanceTimelineProjectionRecord(
						"event-1",
						"INCIDENT_TRANSITIONED",
						occurredAt,
						"INCIDENT_LIFECYCLE",
						"source-1",
						"incident-1",
						"recommendation-1",
						"learning-1",
						"knowledge-1",
						"INFO",
						"SYSTEM",
						"INCIDENT",
						"Incident transitioned",
						"Incident moved to acknowledged state.",
						Map.of("state", "ACKNOWLEDGED"),
						false,
						createdAt
				);

		assertThat(record.eventId()).isEqualTo("event-1");
		assertThat(record.eventType()).isEqualTo("INCIDENT_TRANSITIONED");
		assertThat(record.occurredAt()).isEqualTo(occurredAt);
		assertThat(record.sourceType()).isEqualTo("INCIDENT_LIFECYCLE");
		assertThat(record.sourceId()).isEqualTo("source-1");
		assertThat(record.incidentId()).isEqualTo("incident-1");
		assertThat(record.recommendationRecordId()).isEqualTo("recommendation-1");
		assertThat(record.learningCandidateId()).isEqualTo("learning-1");
		assertThat(record.knowledgeUpdateApplicationId()).isEqualTo("knowledge-1");
		assertThat(record.severity()).isEqualTo("INFO");
		assertThat(record.actorType()).isEqualTo("SYSTEM");
		assertThat(record.resourceType()).isEqualTo("INCIDENT");
		assertThat(record.title()).isEqualTo("Incident transitioned");
		assertThat(record.summary()).isEqualTo("Incident moved to acknowledged state.");
		assertThat(record.metadata()).containsEntry("state", "ACKNOWLEDGED");
		assertThat(record.degraded()).isFalse();
		assertThat(record.createdAt()).isEqualTo(createdAt);
	}

	@Test
	void shouldCopyMetadataDefensively() {
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("state", "OPEN");

		GovernanceTimelineProjectionRecord record =
				new GovernanceTimelineProjectionRecord(
						"event-1",
						"INCIDENT_TRANSITIONED",
						Instant.parse("2026-05-17T00:00:00Z"),
						"INCIDENT_LIFECYCLE",
						"source-1",
						null,
						null,
						null,
						null,
						"INFO",
						"SYSTEM",
						"INCIDENT",
						"title",
						"summary",
						metadata,
						false,
						Instant.parse("2026-05-17T00:00:01Z")
				);

		metadata.put("state", "RESOLVED");

		assertThat(record.metadata()).containsEntry("state", "OPEN");
		assertThatThrownBy(() -> record.metadata().put("state", "MUTATED"))
				.isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	void shouldUseEmptyMetadataWhenNull() {
		GovernanceTimelineProjectionRecord record =
				new GovernanceTimelineProjectionRecord(
						"event-1",
						"INCIDENT_TRANSITIONED",
						Instant.parse("2026-05-17T00:00:00Z"),
						"INCIDENT_LIFECYCLE",
						"source-1",
						null,
						null,
						null,
						null,
						"INFO",
						"SYSTEM",
						"INCIDENT",
						"title",
						"summary",
						null,
						false,
						Instant.parse("2026-05-17T00:00:01Z")
				);

		assertThat(record.metadata()).isEmpty();
	}
}
