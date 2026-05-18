package com.fintech.sre.agent.governance.search;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class GovernanceConsoleSearchContractResourceTest {

	@Test
	void shouldContainSearchContract() throws IOException {
		Path document = Path.of("docs", "governance-console-search-contract.md");

		assertThat(Files.exists(document)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);

		assertThat(markdown).contains("Governance Console Search Contract");
		assertThat(markdown).contains("/internal/governance/search");
		assertThat(markdown).contains("INCIDENT");
		assertThat(markdown).contains("RECOMMENDATION");
		assertThat(markdown).contains("read-only");
		assertThat(markdown).contains("LLM search");
		assertThat(markdown).contains("Qdrant search");
		assertThat(markdown).contains("metadata");
		assertThat(markdown).contains("payload");
		assertThat(markdown).contains("rawLog");
	}
}
