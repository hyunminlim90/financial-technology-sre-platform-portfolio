package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class ReactGovernanceTimelineImplementationReadinessChecklistResourceTest {

	@Test
	void shouldContainReactTimelineImplementationReadinessChecklist()
			throws IOException {
		Path document = Path.of(
				"docs",
				"react-governance-timeline-implementation-readiness-checklist.md"
		);
		Path readme = Path.of("README.md");

		assertThat(Files.exists(document)).isTrue();
		assertThat(Files.exists(readme)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);
		String readmeMarkdown = Files.readString(readme, StandardCharsets.UTF_8);

		assertThat(markdown).contains(
				"# React Governance Timeline Implementation Readiness Checklist"
		);
		assertThat(markdown).contains("Backend Timeline API Readiness");
		assertThat(markdown).contains("Timeline Type Contract Readiness");
		assertThat(markdown).contains("Timeline API Client Contract Readiness");
		assertThat(markdown).contains("Timeline State Contract Readiness");
		assertThat(markdown).contains("Timeline Rendering Contract Readiness");
		assertThat(markdown).contains("Timeline Interaction Contract Readiness");
		assertThat(markdown).contains("Timeline Accessibility Contract Readiness");
		assertThat(markdown).contains("Security and Read-only Readiness");
		assertThat(markdown).contains("Forbidden Mutation Readiness");
		assertThat(markdown).contains("React Project Creation Trigger");
		assertThat(markdown).contains("Non-goals");
		assertThat(markdown).contains("/internal/governance/timeline");
		assertThat(markdown).contains("Repeated eventType serialization documented.");
		assertThat(markdown).contains("Navigation-only semantics documented.");
		assertThat(markdown).contains("approve interaction excluded.");
		assertThat(markdown).contains("NEXT/PREVIOUS semantics implemented.");
		assertThat(markdown).contains("Empty/loading/error rendering documented.");
		assertThat(markdown).contains("kubectl/ArgoCD/GitOps mutation excluded.");
		assertThat(markdown).contains("RAG/Qdrant mutation excluded.");
		assertThat(markdown).contains("Suggested future project path: `apps/governance-console`.");
		assertThat(readmeMarkdown).contains(
				"### React Governance Timeline Implementation Readiness Checklist"
		);
		assertThat(readmeMarkdown).contains(
				"docs/react-governance-timeline-implementation-readiness-checklist.md"
		);
	}
}
