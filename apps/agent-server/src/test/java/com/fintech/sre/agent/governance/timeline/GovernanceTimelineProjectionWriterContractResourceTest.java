package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class GovernanceTimelineProjectionWriterContractResourceTest {

	@Test
	void shouldContainTimelineProjectionWriterContract() throws IOException {
		Path document = Path.of(
				"docs",
				"governance-timeline-projection-writer-contract.md"
		);
		Path readme = Path.of("README.md");

		assertThat(Files.exists(document)).isTrue();
		assertThat(Files.exists(readme)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);
		String readmeMarkdown = Files.readString(readme, StandardCharsets.UTF_8);

		assertThat(markdown).contains(
				"# Governance Timeline Projection Writer Contract"
		);
		assertThat(markdown).contains("source record");
		assertThat(markdown).contains("projection mapper");
		assertThat(markdown).contains("sanitization");
		assertThat(markdown).contains("projection writer");
		assertThat(markdown).contains("projection store");
		assertThat(markdown).contains("append-oriented projection write behavior");
		assertThat(markdown).contains("historical audit mutation prohibition");
		assertThat(markdown).contains("`event_id` uniqueness");
		assertThat(markdown).contains("minimal projection overwrite behavior");
		assertThat(markdown).contains("`event_id`-based idempotent write behavior");
		assertThat(markdown).contains("duplicate projection row creation is not allowed");
		assertThat(markdown).contains("retry-safe write behavior is supported");
		assertThat(markdown).contains("`occurredAt DESC, eventId DESC` ordering");
		assertThat(markdown).contains("cursor ordering compatibility");
		assertThat(markdown).contains("stable pagination compatibility");
		assertThat(markdown).contains("sanitization is mandatory before projection write");
		assertThat(markdown).contains("secrets, tokens, and passwords must not be stored");
		assertThat(markdown).contains("payment data and customer PII must not be stored");
		assertThat(markdown).contains("raw prompts and raw LLM responses must not be stored");
		assertThat(markdown).contains("stack traces and raw logs must not be stored");
		assertThat(markdown).contains("partial projection write failure is allowed");
		assertThat(markdown).contains("best-effort degraded projection is preserved");
		assertThat(markdown).contains("failed projection source isolation remains visible");
		assertThat(markdown).contains("external exception detail exposure is not allowed");
		assertThat(markdown).contains("idempotent retry");
		assertThat(markdown).contains("at-least-once retry compatibility");
		assertThat(markdown).contains("`projection_write_total`");
		assertThat(markdown).contains("`projection_write_failure_total`");
		assertThat(markdown).contains("`projection_write_degraded_total`");
		assertThat(markdown).contains("low-cardinality tag discipline");
		assertThat(markdown).contains("`eventId`");
		assertThat(markdown).contains("runtime aggregation may migrate toward projection persistence");
		assertThat(markdown).contains("API compatibility must remain stable");
		assertThat(markdown).contains("frontend cursor compatibility must remain stable");
		assertThat(markdown).contains("actual projection writer implementation");
		assertThat(markdown).contains("R2DBC write repository");
		assertThat(markdown).contains("Kafka pipeline");
		assertThat(markdown).contains("CDC");
		assertThat(markdown).contains("Debezium");
		assertThat(markdown).contains("exactly-once distributed guarantee");
		assertThat(markdown).contains("event sourcing migration");
		assertThat(markdown).contains("SSE");
		assertThat(markdown).contains("WebSocket");
		assertThat(readmeMarkdown).contains(
				"### Governance Timeline Projection Writer Contract"
		);
		assertThat(readmeMarkdown).contains(
				"docs/governance-timeline-projection-writer-contract.md"
		);
	}
}
