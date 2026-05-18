package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class GovernanceTimelineResilienceContractResourceTest {

	@Test
	void shouldContainTimelineResilienceContract() throws IOException {
		Path document = Path.of("docs", "governance-timeline-resilience-contract.md");

		assertThat(Files.exists(document)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);

		assertThat(markdown).contains("# Governance Timeline Resilience Contract");
		assertThat(markdown).contains("STRICT");
		assertThat(markdown).contains("PARTIAL_DEGRADED");
		assertThat(markdown).contains("FAIL_OPEN_READ_ONLY");
		assertThat(markdown).contains("component_query_failed");
		assertThat(markdown).contains("timeline_query_timeout");
		assertThat(markdown).contains("read-only");
		assertThat(markdown).contains("Append-only");
		assertThat(markdown).contains("GitOps");
		assertThat(markdown).contains("RAG");
		assertThat(markdown).contains("Qdrant");
		assertThat(markdown).contains("stack traces");
		assertThat(markdown).contains("WebFlux streaming");
	}
}
