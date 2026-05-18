package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class GovernanceTimelineProjectionBackedAggregationFinalConsistencyChecklistResourceTest {

	@Test
	void shouldContainProjectionBackedAggregationFinalConsistencyChecklist()
			throws IOException {
		Path document = Path.of(
				"docs",
				"governance-timeline-projection-backed-aggregation-final-consistency-checklist.md"
		);
		Path readme = Path.of("README.md");

		assertThat(Files.exists(document)).isTrue();
		assertThat(Files.exists(readme)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);
		String readmeMarkdown = Files.readString(readme, StandardCharsets.UTF_8);

		assertThat(markdown).contains(
				"# Governance Timeline Projection-backed Aggregation Final Consistency Checklist"
		);
		assertThat(markdown).contains("Projection Write Path Consistency");
		assertThat(markdown).contains("Projection Query Path Consistency");
		assertThat(markdown).contains("Ordering and Cursor Semantics Consistency");
		assertThat(markdown).contains("Metrics Low-cardinality Consistency");
		assertThat(markdown).contains("Health and Runtime Summary Consistency");
		assertThat(markdown).contains("Aggregation Routing Compatibility");
		assertThat(markdown).contains("Runtime Fan-out Compatibility");
		assertThat(markdown).contains("Read-only Governance Boundary Consistency");
		assertThat(markdown).contains("Future R2DBC Migration Compatibility");
		assertThat(markdown).contains("Non-goals Consistency");
		assertThat(markdown).contains(
				"`GovernanceTimelineProjection` maps to `GovernanceTimelineProjectionRecord`."
		);
		assertThat(markdown).contains(
				"`GovernanceTimelineProjectionStore.findRecent(...)` feeds the projection query adapter."
		);
		assertThat(markdown).contains("Ordering remains `occurredAt DESC, eventId DESC`.");
		assertThat(markdown).contains("`NEXT` returns older events.");
		assertThat(markdown).contains("`PREVIOUS` returns newer events.");
		assertThat(markdown).contains(
				"Invalid cursor fails with `GovernanceTimelineCursorDecodeException`."
		);
		assertThat(markdown).contains(
				"Projection query metrics expose result and direction only."
		);
		assertThat(markdown).contains(
				"Projection query health does not execute actual queries."
		);
		assertThat(markdown).contains(
				"Projection query runtime summary only composes health."
		);
		assertThat(markdown).contains("`RUNTIME_FAN_OUT` remains the default.");
		assertThat(markdown).contains(
				"`PROJECTION_BACKED` remains explicit future mode."
		);
		assertThat(markdown).contains(
				"Existing runtime fan-out aggregation remains untouched."
		);
		assertThat(markdown).contains(
				"Projection-backed path remains read-model only."
		);
		assertThat(markdown).contains(
				"No GitOps, ArgoCD, Kubernetes, RAG, or Qdrant mutation is introduced."
		);
		assertThat(markdown).contains(
				"Projection store interface can be backed by a future R2DBC implementation."
		);
		assertThat(markdown).contains(
				"In-memory projection store remains a test and local implementation only."
		);
		assertThat(markdown).contains("No controller wiring switch is introduced.");
		assertThat(markdown).contains("No `@Primary` activation is introduced.");
		assertThat(markdown).contains("No production projection-backed activation is introduced.");
		assertThat(readmeMarkdown).contains(
				"### Governance Timeline Projection-backed Aggregation Final Consistency Checklist"
		);
		assertThat(readmeMarkdown).contains(
				"docs/governance-timeline-projection-backed-aggregation-final-consistency-checklist.md"
		);
	}
}
