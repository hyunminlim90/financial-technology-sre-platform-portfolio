package com.fintech.sre.agent.governance.console;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class GovernanceConsoleRuntimeBannerContractResourceTest {

	@Test
	void shouldContainRuntimeBannerContract() throws IOException {
		Path document = Path.of("docs", "governance-console-runtime-banner-contract.md");

		assertThat(Files.exists(document)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);

		assertThat(markdown).contains("# Governance Console Runtime Banner Contract");
		assertThat(markdown).contains("/internal/governance/console/runtime-summary");
		assertThat(markdown).contains("NORMAL");
		assertThat(markdown).contains("DEGRADED_READ_ONLY");
		assertThat(markdown).contains("ATTENTION_REQUIRED");
		assertThat(markdown).contains("degradedSignals");
		assertThat(markdown).contains("read-only");
		assertThat(markdown).contains("internal-only");
		assertThat(markdown).contains("approve recommendations");
		assertThat(markdown).contains("trigger remediation");
		assertThat(markdown).contains("GitOps");
		assertThat(markdown).contains("RAG");
		assertThat(markdown).contains("Qdrant");
	}
}
