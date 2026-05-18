package com.fintech.sre.agent.governance.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class GovernanceDashboardNavigationContractResourceTest {

	@Test
	void shouldContainReadOnlyNavigationContract() throws IOException {
		Path document = Path.of("docs", "governance-dashboard-navigation-contract.md");

		assertThat(Files.exists(document)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);

		assertThat(markdown).contains("# Governance Dashboard Navigation Contract");
		assertThat(markdown).contains("/internal/governance/dashboard/overview");
		assertThat(markdown).contains("/internal/governance/dashboard/summary");
		assertThat(markdown).contains("/internal/governance/dashboard/backlog");
		assertThat(markdown).contains("/internal/governance/dashboard/trends");
		assertThat(markdown).contains("/internal/governance/dashboard/risk-indicators");
		assertThat(markdown).contains("/internal/governance/dashboard/health");
		assertThat(markdown).contains("read-only");
		assertThat(markdown).contains("internal-only");
		assertThat(markdown).contains("must not");
		assertThat(markdown).contains("remediation");
		assertThat(markdown).contains("GitOps");
		assertThat(markdown).contains("RAG");
		assertThat(markdown).contains("Qdrant");
	}
}
