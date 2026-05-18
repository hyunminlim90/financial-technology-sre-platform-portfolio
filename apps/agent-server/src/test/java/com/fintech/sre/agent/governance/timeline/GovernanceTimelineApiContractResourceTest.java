package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class GovernanceTimelineApiContractResourceTest {

	@Test
	void shouldContainTimelineApiContract() throws IOException {
		Path document = Path.of("docs", "governance-timeline-api-contract.md");

		assertThat(Files.exists(document)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);

		assertThat(markdown).contains("# Governance Timeline API Contract");
		assertThat(markdown).contains("/internal/governance/timeline");
		assertThat(markdown).contains("/internal/governance/timeline/health");
		assertThat(markdown).contains("/internal/governance/timeline/runtime-summary");
		assertThat(markdown).contains("Response Envelope");
		assertThat(markdown).contains("opaque");
		assertThat(markdown).contains("DEGRADED");
		assertThat(markdown).contains("errors");
		assertThat(markdown).contains("read-only");
		assertThat(markdown).contains("internal-only");
		assertThat(markdown).contains("SSE");
		assertThat(markdown).contains("WebSocket");
	}
}
