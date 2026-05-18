package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class GovernanceTimelineProjectionStoreContractResourceTest {

	@Test
	void shouldContainTimelineProjectionStoreContract() throws IOException {
		Path document = Path.of(
				"docs",
				"governance-timeline-projection-store-contract.md"
		);
		Path readme = Path.of("README.md");

		assertThat(Files.exists(document)).isTrue();
		assertThat(Files.exists(readme)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);
		String readmeMarkdown = Files.readString(readme, StandardCharsets.UTF_8);

		assertThat(markdown).contains(
				"# Governance Timeline Projection Store Contract"
		);
		assertThat(markdown).contains("future contract for a materialized Governance");
		assertThat(markdown).contains("Timeline projection store");
		assertThat(markdown).contains("runtime aggregation");
		assertThat(markdown).contains("materialized timeline projection storage");
		assertThat(markdown).contains("append-only event projection rows");
		assertThat(markdown).contains("read-optimized query paths");
		assertThat(markdown).contains("`eventId` uniqueness");
		assertThat(markdown).contains("append-only historical audit behavior");
		assertThat(markdown).contains("`occurredAt DESC, eventId DESC` ordering");
		assertThat(markdown).contains("opaque cursor stability");
		assertThat(markdown).contains("`NEXT` semantics for older events");
		assertThat(markdown).contains("`PREVIOUS` semantics for newer events");
		assertThat(markdown).contains("Historical audit mutation is not allowed");
		assertThat(markdown).contains("partial projection availability is allowed");
		assertThat(markdown).contains("best-effort degraded read availability is preserved");
		assertThat(markdown).contains("exception details must not be exposed");
		assertThat(markdown).contains("read-only");
		assertThat(markdown).contains("append-only in audit semantics");
		assertThat(markdown).contains("runtime aggregation may migrate to a projection store");
		assertThat(markdown).contains("frontend and API contract stability must be preserved");
		assertThat(markdown).contains("actual projection table schema");
		assertThat(markdown).contains("R2DBC repository implementation");
		assertThat(markdown).contains("CDC implementation");
		assertThat(markdown).contains("Kafka projection pipeline");
		assertThat(markdown).contains("Debezium");
		assertThat(markdown).contains("event sourcing migration");
		assertThat(markdown).contains("SSE");
		assertThat(markdown).contains("WebSocket");
		assertThat(readmeMarkdown).contains(
				"### Governance Timeline Projection Store Contract"
		);
		assertThat(readmeMarkdown).contains(
				"docs/governance-timeline-projection-store-contract.md"
		);
	}
}
