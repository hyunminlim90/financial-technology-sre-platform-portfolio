package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class GovernanceTimelineReadmeUsageResourceTest {

	@Test
	void shouldContainTimelineReadmeUsageSection() throws IOException {
		Path document = Path.of("README.md");

		assertThat(Files.exists(document)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);

		assertThat(markdown).contains("## Governance Timeline API Usage");
		assertThat(markdown).contains("/internal/governance/timeline?limit=50");
		assertThat(markdown).contains("/internal/governance/timeline?cursor={opaqueCursor}&direction=NEXT");
		assertThat(markdown).contains("/internal/governance/timeline?cursor={opaqueCursor}&direction=PREVIOUS");
		assertThat(markdown).contains("/internal/governance/timeline/health");
		assertThat(markdown).contains("/internal/governance/timeline/runtime-summary");
		assertThat(markdown).contains("internal-only");
		assertThat(markdown).contains("read-only");
		assertThat(markdown).contains("append-only");
		assertThat(markdown).contains("opaque");
		assertThat(markdown).contains("remediation");
		assertThat(markdown).contains("GitOps");
		assertThat(markdown).contains("RAG");
		assertThat(markdown).contains("Qdrant");
		assertThat(markdown).contains("docs/governance-timeline-operator-query-guide.md");
	}
}
