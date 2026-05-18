package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class GovernanceTimelineProjectionGovernanceBoundaryContractResourceTest {

	@Test
	void shouldContainTimelineProjectionGovernanceBoundaryContract()
			throws IOException {
		Path document = Path.of(
				"docs",
				"governance-timeline-projection-governance-boundary-contract.md"
		);
		Path readme = Path.of("README.md");

		assertThat(Files.exists(document)).isTrue();
		assertThat(Files.exists(readme)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);
		String readmeMarkdown = Files.readString(readme, StandardCharsets.UTF_8);

		assertThat(markdown).contains(
				"# Governance Timeline Projection Governance Boundary Contract"
		);
		assertThat(markdown).contains("clear projection subsystem governance boundary definition");
		assertThat(markdown).contains("append-only audit continuity preservation");
		assertThat(markdown).contains("operator-facing informational semantics preservation");
		assertThat(markdown).contains("The projection subsystem is read-model only.");
		assertThat(markdown).contains("not a decision engine");
		assertThat(markdown).contains("not an execution engine");
		assertThat(markdown).contains("not an approval orchestration engine");
		assertThat(markdown).contains("Projection query behavior remains read-only.");
		assertThat(markdown).contains("state mutation semantics");
		assertThat(markdown).contains("write-side governance execution");
		assertThat(markdown).contains("historical overwrite is not allowed");
		assertThat(markdown).contains("append-only audit continuity remains preserved");
		assertThat(markdown).contains("historical audit mutation remains minimized");
		assertThat(markdown).contains("operator-facing informational semantics only");
		assertThat(markdown).contains("auto-remediation semantics");
		assertThat(markdown).contains("decision automation semantics");
		assertThat(markdown).contains("best-effort degraded availability remains allowed");
		assertThat(markdown).contains("partial degraded visibility remains preserved");
		assertThat(markdown).contains("failed source isolation remains preserved");
		assertThat(markdown).contains("Replay and recovery remain read-model rebuild mechanisms only.");
		assertThat(markdown).contains("trigger governance actions");
		assertThat(markdown).contains("execute approvals");
		assertThat(markdown).contains("trigger remediation");
		assertThat(markdown).contains("mutate Kubernetes through kubectl");
		assertThat(markdown).contains("mutate GitOps repositories");
		assertThat(markdown).contains("trigger ArgoCD sync");
		assertThat(markdown).contains("mutate RAG");
		assertThat(markdown).contains("mutate Qdrant");
		assertThat(markdown).contains("execute LLM actions");
		assertThat(markdown).contains("runtime aggregation to persistent projection migration compatibility remains");
		assertThat(markdown).contains("projection replay and recovery compatibility remains preserved");
		assertThat(markdown).contains("frontend and API compatibility remains preserved");
		assertThat(markdown).contains("cursor and ordering contract remains preserved");
		assertThat(markdown).contains("execution orchestration");
		assertThat(markdown).contains("decision automation");
		assertThat(markdown).contains("incident remediation engine");
		assertThat(markdown).contains("distributed workflow engine");
		assertThat(markdown).contains("autonomous AI governance execution");
		assertThat(readmeMarkdown).contains(
				"### Governance Timeline Projection Governance Boundary Contract"
		);
		assertThat(readmeMarkdown).contains(
				"docs/governance-timeline-projection-governance-boundary-contract.md"
		);
	}
}
