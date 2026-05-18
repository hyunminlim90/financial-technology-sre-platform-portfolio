package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class GovernanceTimelineHealthContractResourceTest {

	@Test
	void shouldContainTimelineHealthContract() throws IOException {
		Path document = Path.of("docs", "governance-timeline-health-contract.md");

		assertThat(Files.exists(document)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);

		assertThat(markdown).contains("# Governance Timeline Health Contract");
		assertThat(markdown).contains("HEALTHY");
		assertThat(markdown).contains("DEGRADED_CAPABLE");
		assertThat(markdown).contains("STRICT");
		assertThat(markdown).contains("UNAVAILABLE");
		assertThat(markdown).contains("Lightweight Evaluation");
		assertThat(markdown).contains("component_query_timeout");
		assertThat(markdown).contains("read-only");
		assertThat(markdown).contains("observability-only");
		assertThat(markdown).contains("customer data");
		assertThat(markdown).contains("cursor values");
		assertThat(markdown).contains("Prometheus alert rules");
	}
}
