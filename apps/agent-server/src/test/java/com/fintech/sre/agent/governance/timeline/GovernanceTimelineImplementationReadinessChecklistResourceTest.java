package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class GovernanceTimelineImplementationReadinessChecklistResourceTest {

	@Test
	void shouldContainTimelineImplementationReadinessChecklist() throws IOException {
		Path document = Path.of(
				"docs",
				"governance-timeline-implementation-readiness-checklist.md"
		);

		assertThat(Files.exists(document)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);

		assertThat(markdown).contains("# Governance Timeline Implementation Readiness Checklist");
		assertThat(markdown).contains("governance-timeline-pagination-contract.md");
		assertThat(markdown).contains("governance-timeline-query-contract.md");
		assertThat(markdown).contains("governance-timeline-read-model-contract.md");
		assertThat(markdown).contains("governance-timeline-mapping-contract.md");
		assertThat(markdown).contains("governance-timeline-aggregation-contract.md");
		assertThat(markdown).contains("governance-timeline-resilience-contract.md");
		assertThat(markdown).contains("governance-timeline-metrics-contract.md");
		assertThat(markdown).contains("governance-timeline-health-contract.md");
		assertThat(markdown).contains("governance-timeline-runtime-contract.md");
		assertThat(markdown).contains("governance-timeline-frontend-integration-contract.md");
		assertThat(markdown).contains("governance-timeline-api-contract.md");
		assertThat(markdown).contains("Projection Mapper Readiness");
		assertThat(markdown).contains("Cursor Encoding Readiness");
		assertThat(markdown).contains("Resilience Readiness");
		assertThat(markdown).contains("Metrics Readiness");
		assertThat(markdown).contains("Phase 1: in-memory timeline aggregation from existing stores");
		assertThat(markdown).contains("Kafka");
		assertThat(markdown).contains("Qdrant update");
	}
}
