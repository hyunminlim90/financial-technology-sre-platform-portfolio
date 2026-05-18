package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class GovernanceTimelineFinalConsistencyChecklistResourceTest {

	@Test
	void shouldContainTimelineFinalConsistencyChecklist() throws IOException {
		Path document = Path.of(
				"docs",
				"governance-timeline-final-consistency-checklist.md"
		);

		assertThat(Files.exists(document)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);

		assertThat(markdown).contains("# Governance Timeline Final Consistency Checklist");
		assertThat(markdown).contains("API Surface Consistency");
		assertThat(markdown).contains("Cursor Semantics Consistency");
		assertThat(markdown).contains("Ordering Consistency");
		assertThat(markdown).contains("Projection Mapping Consistency");
		assertThat(markdown).contains("Metrics Low-cardinality Consistency");
		assertThat(markdown).contains("Health and Runtime Consistency");
		assertThat(markdown).contains("Security and Read-only Consistency");
		assertThat(markdown).contains("Non-goals Still Excluded");
		assertThat(markdown).contains("GET /internal/governance/timeline");
		assertThat(markdown).contains("GET /internal/governance/timeline/health");
		assertThat(markdown).contains("GET /internal/governance/timeline/runtime-summary");
		assertThat(markdown).contains("Base64 URL-safe");
		assertThat(markdown).contains("occurredAt DESC, eventId DESC");
		assertThat(markdown).contains("GovernanceTimelineEvent");
		assertThat(markdown).contains("low-cardinality");
		assertThat(markdown).contains("timelineRuntime");
		assertThat(markdown).contains("internal-only");
		assertThat(markdown).contains("read-only");
		assertThat(markdown).contains("GitOps");
		assertThat(markdown).contains("Qdrant");
		assertThat(markdown).contains("R2DBC optimized timeline query");
		assertThat(markdown).contains("PrometheusRule");
	}
}
