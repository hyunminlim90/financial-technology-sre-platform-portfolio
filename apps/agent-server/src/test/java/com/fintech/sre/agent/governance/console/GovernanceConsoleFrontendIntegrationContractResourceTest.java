package com.fintech.sre.agent.governance.console;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class GovernanceConsoleFrontendIntegrationContractResourceTest {

	@Test
	void shouldContainFrontendIntegrationContract() throws IOException {
		Path document = Path.of(
				"docs",
				"governance-console-frontend-integration-contract.md"
		);

		assertThat(Files.exists(document)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);

		assertThat(markdown).contains("# Governance Console Frontend Integration Contract");
		assertThat(markdown).contains("/internal/governance/console/runtime-summary");
		assertThat(markdown).contains("timelineRuntime");
		assertThat(markdown).contains("/internal/governance/dashboard/overview");
		assertThat(markdown).contains("/internal/governance/timeline");
		assertThat(markdown).contains("/internal/governance/timeline/health");
		assertThat(markdown).contains("/internal/governance/timeline/runtime-summary");
		assertThat(markdown).contains("/internal/governance/details/overview/incidents/{incidentId}");
		assertThat(markdown).contains("/internal/governance/details/incidents/{incidentId}");
		assertThat(markdown).contains("/internal/governance/search?q={keyword}&type=ALL&window=24h&limit=20");
		assertThat(markdown).contains("degraded=true");
		assertThat(markdown).contains("informational only");
		assertThat(markdown).contains("Timeline UX Rules");
		assertThat(markdown).contains("Timeline runtime summary");
		assertThat(markdown).contains("read-only");
		assertThat(markdown).contains("approve recommendations");
		assertThat(markdown).contains("trigger remediation");
		assertThat(markdown).contains("GitOps");
		assertThat(markdown).contains("RAG");
		assertThat(markdown).contains("Qdrant");
		assertThat(markdown).contains("internal");
		assertThat(markdown).contains("Polling Guidance");
	}
}
