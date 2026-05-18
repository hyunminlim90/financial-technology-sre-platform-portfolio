package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class GovernanceTimelineOperatorQueryGuideResourceTest {

	@Test
	void shouldContainTimelineOperatorQueryGuide() throws IOException {
		Path document = Path.of(
				"docs",
				"governance-timeline-operator-query-guide.md"
		);

		assertThat(Files.exists(document)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);

		assertThat(markdown).contains("# Governance Timeline Operator Query Guide");
		assertThat(markdown).contains("GET /internal/governance/timeline?limit=50");
		assertThat(markdown).contains("GET /internal/governance/timeline?cursor=<opaque>&direction=NEXT");
		assertThat(markdown).contains("GET /internal/governance/timeline?cursor=<opaque>&direction=PREVIOUS");
		assertThat(markdown).contains("GET /internal/governance/timeline/runtime-summary");
		assertThat(markdown).contains("GET /internal/governance/timeline/health");
		assertThat(markdown).contains("older events");
		assertThat(markdown).contains("newer events");
		assertThat(markdown).contains("opaque Base64 URL-safe tokens");
		assertThat(markdown).contains("occurredAt DESC, eventId DESC");
		assertThat(markdown).contains("DEGRADED_READ_ONLY");
		assertThat(markdown).contains("ATTENTION_REQUIRED");
		assertThat(markdown).contains("DEGRADED_CAPABLE");
		assertThat(markdown).contains("fin_sre_governance_timeline_query_total");
		assertThat(markdown).contains("fin_sre_governance_timeline_runtime_mode");
		assertThat(markdown).contains("internal-only");
		assertThat(markdown).contains("read-only");
		assertThat(markdown).contains("GitOps");
		assertThat(markdown).contains("Kubernetes");
		assertThat(markdown).contains("secrets");
		assertThat(markdown).contains("raw logs");
	}
}
