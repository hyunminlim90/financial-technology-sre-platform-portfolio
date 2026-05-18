package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class GovernanceTimelineQueryStoreContractResourceTest {

	@Test
	void shouldContainTimelineQueryStoreContract() throws IOException {
		Path document = Path.of(
				"docs",
				"governance-timeline-query-store-contract.md"
		);
		Path readme = Path.of("README.md");

		assertThat(Files.exists(document)).isTrue();
		assertThat(Files.exists(readme)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);
		String readmeMarkdown = Files.readString(readme, StandardCharsets.UTF_8);

		assertThat(markdown).contains("# Governance Timeline Query Store Contract");
		assertThat(markdown).contains("in-memory aggregation and");
		assertThat(markdown).contains("fan-out aggregation cost exists");
		assertThat(markdown).contains("R2DBC and PostgreSQL-backed timeline projection queries");
		assertThat(markdown).contains("materialized read model storage");
		assertThat(markdown).contains("OpenSearch or Elasticsearch-backed read/query support");
		assertThat(markdown).contains("occurredAt DESC, eventId DESC");
		assertThat(markdown).contains("`NEXT` continues to return older events");
		assertThat(markdown).contains("`PREVIOUS` continues to return newer events");
		assertThat(markdown).contains("stable pagination behavior remains preserved");
		assertThat(markdown).contains("`eventId`-based deduplication");
		assertThat(markdown).contains("source merge semantics");
		assertThat(markdown).contains("partial degraded query remains supported");
		assertThat(markdown).contains("best-effort read availability remains supported");
		assertThat(markdown).contains("failed source isolation remains visible");
		assertThat(markdown).contains("read-only");
		assertThat(markdown).contains("append-only in audit semantics");
		assertThat(markdown).contains("query latency metrics");
		assertThat(markdown).contains("low-cardinality tag discipline");
		assertThat(markdown).contains("in-memory aggregation may migrate to a persistent query store");
		assertThat(markdown).contains("API contract stability must be preserved during migration");
		assertThat(markdown).contains("frontend cursor contract must remain stable");
		assertThat(markdown).contains("actual R2DBC implementation");
		assertThat(markdown).contains("actual PostgreSQL schema");
		assertThat(markdown).contains("OpenSearch or Elasticsearch integration");
		assertThat(markdown).contains("Kafka streaming");
		assertThat(markdown).contains("SSE");
		assertThat(markdown).contains("WebSocket");
		assertThat(readmeMarkdown).contains("### Governance Timeline Query Store Contract");
		assertThat(readmeMarkdown).contains("docs/governance-timeline-query-store-contract.md");
	}
}
