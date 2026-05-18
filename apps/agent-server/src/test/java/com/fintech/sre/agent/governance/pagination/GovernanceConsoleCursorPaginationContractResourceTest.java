package com.fintech.sre.agent.governance.pagination;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class GovernanceConsoleCursorPaginationContractResourceTest {

	@Test
	void shouldContainCursorPaginationContract() throws IOException {
		Path document = Path.of(
				"docs",
				"governance-console-cursor-pagination-contract.md"
		);

		assertThat(Files.exists(document)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);

		assertThat(markdown).contains("# Governance Console Cursor Pagination Contract");
		assertThat(markdown).contains("opaque");
		assertThat(markdown).contains("occurredAt DESC, recordId DESC");
		assertThat(markdown).contains("Offset pagination is discouraged");
		assertThat(markdown).contains("/internal/governance/search");
		assertThat(markdown).contains("/internal/governance/details/incidents/{incidentId}");
		assertThat(markdown).contains("customer data");
		assertThat(markdown).contains("tokens");
		assertThat(markdown).contains("raw logs");
		assertThat(markdown).contains("Non-goals");
		assertThat(markdown).contains("WebSocket");
	}
}
