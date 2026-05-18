package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class GovernanceTimelineProjectionFinalConsistencyChecklistResourceTest {

	@Test
	void shouldContainTimelineProjectionFinalConsistencyChecklist()
			throws IOException {
		Path document = Path.of(
				"docs",
				"governance-timeline-projection-final-consistency-checklist.md"
		);
		Path readme = Path.of("README.md");

		assertThat(Files.exists(document)).isTrue();
		assertThat(Files.exists(readme)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);
		String readmeMarkdown = Files.readString(readme, StandardCharsets.UTF_8);

		assertThat(markdown).contains(
				"# Governance Timeline Projection Final Consistency Checklist"
		);
		assertThat(markdown).contains("Query and Projection Consistency");
		assertThat(markdown).contains("Ordering and Cursor Consistency");
		assertThat(markdown).contains("Replay and Recovery Consistency");
		assertThat(markdown).contains("Retention and Archive Consistency");
		assertThat(markdown).contains("Observability and Failure Taxonomy Consistency");
		assertThat(markdown).contains("Evolution and Backward Compatibility Consistency");
		assertThat(markdown).contains("Governance Boundary Consistency");
		assertThat(markdown).contains("Operator-facing Semantics Consistency");
		assertThat(markdown).contains("Migration Compatibility Consistency");
		assertThat(markdown).contains("Non-goals Consistency");
		assertThat(markdown).contains("Query store semantics and projection store semantics remain aligned.");
		assertThat(markdown).contains("`occurredAt DESC, eventId DESC` ordering remains preserved.");
		assertThat(markdown).contains("Opaque cursor semantics remain preserved.");
		assertThat(markdown).contains("Stable pagination compatibility remains preserved.");
		assertThat(markdown).contains("Idempotent replay remains preserved.");
		assertThat(markdown).contains("Idempotent recovery remains preserved.");
		assertThat(markdown).contains("Historical rebuild compatibility remains preserved.");
		assertThat(markdown).contains("Retention and replay coexistence consistency remains preserved.");
		assertThat(markdown).contains("Best-effort degraded semantics remain consistent across the projection subsystem.");
		assertThat(markdown).contains("Failure taxonomy and operator visibility remain consistent.");
		assertThat(markdown).contains("Low-cardinality metrics remain preserved.");
		assertThat(markdown).contains("Append-oriented evolution remains preserved.");
		assertThat(markdown).contains("Read-model-only semantics remain preserved.");
		assertThat(markdown).contains("Mutation prohibition remains preserved.");
		assertThat(markdown).contains("Operator-facing informational semantics remain preserved.");
		assertThat(markdown).contains("Auto-remediation semantics remain prohibited.");
		assertThat(markdown).contains("Decision automation semantics remain prohibited.");
		assertThat(markdown).contains("Approval and remediation execution semantics remain prohibited.");
		assertThat(markdown).contains("Runtime aggregation to persistent projection migration compatibility remains preserved.");
		assertThat(markdown).contains("Projection replay and recovery compatibility remains preserved.");
		assertThat(markdown).contains("Frontend, API, and runtime compatibility remain preserved.");
		assertThat(markdown).contains("Execution orchestration is not introduced.");
		assertThat(markdown).contains("Decision automation is not introduced.");
		assertThat(markdown).contains("Autonomous remediation is not introduced.");
		assertThat(markdown).contains("Distributed governance execution is not introduced.");
		assertThat(readmeMarkdown).contains(
				"### Governance Timeline Projection Final Consistency Checklist"
		);
		assertThat(readmeMarkdown).contains(
				"docs/governance-timeline-projection-final-consistency-checklist.md"
		);
	}
}
