package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class GovernanceTimelineReadModelContractResourceTest {

	@Test
	void shouldContainTimelineReadModelContract() throws IOException {
		Path document = Path.of("docs", "governance-timeline-read-model-contract.md");

		assertThat(Files.exists(document)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);

		assertThat(markdown).contains("# Governance Timeline Read Model Contract");
		assertThat(markdown).contains("immutable read-only audit projections");
		assertThat(markdown).contains("append a new event");
		assertThat(markdown).contains("AI-generated recommendation");
		assertThat(markdown).contains("KNOWLEDGE_UPDATE");
		assertThat(markdown).contains("payment payloads");
		assertThat(markdown).contains("tokens");
		assertThat(markdown).contains("full prompts");
		assertThat(markdown).contains("WebFlux streaming");
		assertThat(markdown).contains("SSE");
		assertThat(markdown).contains("Qdrant");
	}
}
