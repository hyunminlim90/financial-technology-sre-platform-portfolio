package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class GovernanceTimelineMetricsContractResourceTest {

	@Test
	void shouldContainTimelineMetricsContract() throws IOException {
		Path document = Path.of("docs", "governance-timeline-metrics-contract.md");

		assertThat(Files.exists(document)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);

		assertThat(markdown).contains("# Governance Timeline Metrics Contract");
		assertThat(markdown).contains("fin_sre_governance_timeline_query_total");
		assertThat(markdown).contains("fin_sre_governance_timeline_aggregation_total");
		assertThat(markdown).contains("fin_sre_governance_timeline_degraded_total");
		assertThat(markdown).contains("fin_sre_governance_timeline_page_size");
		assertThat(markdown).contains("fin_sre_governance_timeline_health_status");
		assertThat(markdown).contains("component_query_timeout");
		assertThat(markdown).contains("cursor");
		assertThat(markdown).contains("git commit SHA");
		assertThat(markdown).contains("observability-only");
		assertThat(markdown).contains("Prometheus alert rules");
		assertThat(markdown).contains("Grafana dashboards");
	}
}
