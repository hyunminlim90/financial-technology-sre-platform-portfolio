package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class GovernanceTimelineQueryContractResourceTest {

	@Test
	void shouldContainTimelineQueryContract() throws IOException {
		Path document = Path.of("docs", "governance-timeline-query-contract.md");

		assertThat(Files.exists(document)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);

		assertThat(markdown).contains("# Governance Timeline Query Contract");
		assertThat(markdown).contains("RECOMMENDATION_CREATED");
		assertThat(markdown).contains("KNOWLEDGE_UPDATED");
		assertThat(markdown).contains("read-only append-only operational audit views");
		assertThat(markdown).contains("cursor must be opaque");
		assertThat(markdown).contains("trigger remediation");
		assertThat(markdown).contains("mutate ArgoCD");
		assertThat(markdown).contains("customer data");
		assertThat(markdown).contains("full prompts");
		assertThat(markdown).contains("SSE");
		assertThat(markdown).contains("WebSocket");
	}
}
