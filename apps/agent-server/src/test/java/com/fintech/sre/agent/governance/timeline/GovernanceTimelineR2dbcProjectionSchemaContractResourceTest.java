package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class GovernanceTimelineR2dbcProjectionSchemaContractResourceTest {

	@Test
	void shouldContainTimelineR2dbcProjectionSchemaContract()
			throws IOException {
		Path document = Path.of(
				"docs",
				"governance-timeline-r2dbc-projection-schema-contract.md"
		);
		Path readme = Path.of("README.md");

		assertThat(Files.exists(document)).isTrue();
		assertThat(Files.exists(readme)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);
		String readmeMarkdown = Files.readString(readme, StandardCharsets.UTF_8);

		assertThat(markdown).contains(
				"# Governance Timeline R2DBC Projection Schema Contract"
		);
		assertThat(markdown).contains("Recommended required columns");
		assertThat(markdown).contains("`event_id`");
		assertThat(markdown).contains("`event_type`");
		assertThat(markdown).contains("`occurred_at`");
		assertThat(markdown).contains("`source_type`");
		assertThat(markdown).contains("`source_id`");
		assertThat(markdown).contains("`incident_id`");
		assertThat(markdown).contains("`recommendation_record_id`");
		assertThat(markdown).contains("`learning_candidate_id`");
		assertThat(markdown).contains("`knowledge_update_application_id`");
		assertThat(markdown).contains("`severity`");
		assertThat(markdown).contains("`actor_type`");
		assertThat(markdown).contains("`resource_type`");
		assertThat(markdown).contains("`title`");
		assertThat(markdown).contains("`summary`");
		assertThat(markdown).contains("`metadata_json`");
		assertThat(markdown).contains("`degraded`");
		assertThat(markdown).contains("`created_at`");
		assertThat(markdown).contains("sanitized JSONB only");
		assertThat(markdown).contains("arbitrary raw payload storage is not allowed");
		assertThat(markdown).contains("payment information");
		assertThat(markdown).contains("customer PII");
		assertThat(markdown).contains("raw prompts");
		assertThat(markdown).contains("raw LLM responses");
		assertThat(markdown).contains("stack traces");
		assertThat(markdown).contains("raw logs");
		assertThat(markdown).contains("`(occurred_at DESC, event_id DESC)`");
		assertThat(markdown).contains("`(event_type, occurred_at DESC)`");
		assertThat(markdown).contains("`(incident_id, occurred_at DESC)`");
		assertThat(markdown).contains("`(recommendation_record_id, occurred_at DESC)`");
		assertThat(markdown).contains("`GIN(metadata_json)`");
		assertThat(markdown).contains("`occurredAt DESC, eventId DESC` ordering");
		assertThat(markdown).contains("opaque cursor compatibility");
		assertThat(markdown).contains("stable pagination behavior");
		assertThat(markdown).contains("projection row append-only behavior is preserved");
		assertThat(markdown).contains("historical audit mutation is not allowed");
		assertThat(markdown).contains("`event_id` uniqueness is preserved");
		assertThat(markdown).contains("runtime aggregation may migrate to an R2DBC projection-backed query path");
		assertThat(markdown).contains("frontend and API compatibility must remain stable");
		assertThat(markdown).contains("actual PostgreSQL DDL");
		assertThat(markdown).contains("Flyway migration");
		assertThat(markdown).contains("Liquibase migration");
		assertThat(markdown).contains("R2DBC repository");
		assertThat(markdown).contains("projection writer");
		assertThat(markdown).contains("CDC pipeline");
		assertThat(markdown).contains("Kafka");
		assertThat(markdown).contains("Debezium");
		assertThat(markdown).contains("SSE");
		assertThat(markdown).contains("WebSocket");
		assertThat(readmeMarkdown).contains(
				"### Governance Timeline R2DBC Projection Schema Contract"
		);
		assertThat(readmeMarkdown).contains(
				"docs/governance-timeline-r2dbc-projection-schema-contract.md"
		);
	}
}
