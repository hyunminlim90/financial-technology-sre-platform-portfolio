package com.fintech.sre.agent.governance.detail;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class GovernanceConsoleApiContractSummaryResourceTest {

	@Test
	void shouldContainConsoleApiContractSummary() throws IOException {
		Path document = Path.of("docs", "governance-console-api-contract-summary.md");

		assertThat(Files.exists(document)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);

		assertThat(markdown).contains("# Governance Console API Contract Summary");
		assertThat(markdown).contains("/internal/governance/dashboard/overview");
		assertThat(markdown).contains("/internal/governance/dashboard/health");
		assertThat(markdown).contains("/internal/governance/details/incidents/{incidentId}");
		assertThat(markdown).contains("/internal/governance/details/health");
		assertThat(markdown).contains("/internal/governance/details/overview/incidents/{incidentId}");
		assertThat(markdown).contains("/internal/governance/timeline");
		assertThat(markdown).contains("/internal/governance/timeline/health");
		assertThat(markdown).contains("/internal/governance/timeline/runtime-summary");
		assertThat(markdown).contains("timelineRuntime");
		assertThat(markdown).contains("Console runtime banner");
		assertThat(markdown).contains("fin_sre_governance_dashboard_health_status");
		assertThat(markdown).contains("fin_sre_governance_detail_health_status");
		assertThat(markdown).contains("fin_sre_governance_detail_overview_query_total");
		assertThat(markdown).contains("fin_sre_governance_timeline_health_status");
		assertThat(markdown).contains("fin_sre_governance_timeline_runtime_mode");
		assertThat(markdown).contains("read-only");
		assertThat(markdown).contains("internal-only");
		assertThat(markdown).contains("sensitive payloads");
		assertThat(markdown).contains("tokens");
		assertThat(markdown).contains("raw logs");
		assertThat(markdown).contains("Qdrant");
	}
}
