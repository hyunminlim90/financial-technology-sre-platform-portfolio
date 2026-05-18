package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class GovernanceTimelineFrontendIntegrationContractResourceTest {

	@Test
	void shouldContainTimelineFrontendIntegrationContract() throws IOException {
		Path document = Path.of(
				"docs",
				"governance-timeline-frontend-integration-contract.md"
		);

		assertThat(Files.exists(document)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);

		assertThat(markdown).contains("# Governance Timeline Frontend Integration Contract");
		assertThat(markdown).contains("opaque");
		assertThat(markdown).contains("Infinite Scroll Rules");
		assertThat(markdown).contains("degraded=true");
		assertThat(markdown).contains("Event Severity Rendering");
		assertThat(markdown).contains("Actor and Resource Rendering");
		assertThat(markdown).contains("read-only");
		assertThat(markdown).contains("approve recommendations");
		assertThat(markdown).contains("trigger remediation");
		assertThat(markdown).contains("cursor values");
		assertThat(markdown).contains("SSE-based live incident timeline");
		assertThat(markdown).contains("Qdrant");
	}
}
