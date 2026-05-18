package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class GovernanceTimelineProjectionBootstrapContractResourceTest {

	@Test
	void shouldContainTimelineProjectionBootstrapContract()
			throws IOException {
		Path document = Path.of(
				"docs",
				"governance-timeline-projection-bootstrap-contract.md"
		);
		Path readme = Path.of("README.md");

		assertThat(Files.exists(document)).isTrue();
		assertThat(Files.exists(readme)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);
		String readmeMarkdown = Files.readString(readme, StandardCharsets.UTF_8);

		assertThat(markdown).contains(
				"# Governance Timeline Projection Bootstrap Contract"
		);
		assertThat(markdown).contains("new projection environment bootstrap");
		assertThat(markdown).contains("projection schema reset");
		assertThat(markdown).contains("cold projection rebuild");
		assertThat(markdown).contains("projection store initialization");
		assertThat(markdown).contains("projection bootstrap recovery");
		assertThat(markdown).contains("`occurredAt DESC, eventId DESC` ordering compatibility");
		assertThat(markdown).contains("cursor compatibility");
		assertThat(markdown).contains("stable pagination compatibility");
		assertThat(markdown).contains("historical ordering compatibility");
		assertThat(markdown).contains("`event_id`-based bootstrap idempotency is preserved");
		assertThat(markdown).contains("duplicate projection creation is not allowed");
		assertThat(markdown).contains("retry-safe bootstrap is allowed");
		assertThat(markdown).contains("Bootstrap is a read-model initialization mechanism only.");
		assertThat(markdown).contains("trigger governance actions");
		assertThat(markdown).contains("execute approvals");
		assertThat(markdown).contains("trigger remediation");
		assertThat(markdown).contains("mutate GitOps repositories");
		assertThat(markdown).contains("mutate Kubernetes");
		assertThat(markdown).contains("trigger ArgoCD sync");
		assertThat(markdown).contains("update RAG");
		assertThat(markdown).contains("update Qdrant");
		assertThat(markdown).contains("best-effort degraded bootstrap is allowed");
		assertThat(markdown).contains("partial bootstrap visibility remains preserved");
		assertThat(markdown).contains("failed bootstrap source isolation remains preserved");
		assertThat(markdown).contains("bootstrap degradation visibility remains preserved");
		assertThat(markdown).contains("`projection_bootstrap_total`");
		assertThat(markdown).contains("`projection_bootstrap_failure_total`");
		assertThat(markdown).contains("`projection_bootstrap_degraded_total`");
		assertThat(markdown).contains("low-cardinality metric discipline");
		assertThat(markdown).contains("raw exception detail");
		assertThat(markdown).contains("tag explosion");
		assertThat(markdown).contains("operator-facing informational semantics only");
		assertThat(markdown).contains("auto-remediation semantics");
		assertThat(markdown).contains("governance action trigger semantics");
		assertThat(markdown).contains("runtime aggregation to persistent projection migration compatibility");
		assertThat(markdown).contains("projection replay and recovery compatibility remains preserved");
		assertThat(markdown).contains("frontend and API compatibility remains preserved");
		assertThat(markdown).contains("cursor contract remains preserved");
		assertThat(markdown).contains("actual bootstrap implementation");
		assertThat(markdown).contains("bootstrap scheduler");
		assertThat(markdown).contains("distributed bootstrap orchestration");
		assertThat(markdown).contains("cross-region bootstrap");
		assertThat(markdown).contains("event sourcing migration");
		assertThat(readmeMarkdown).contains(
				"### Governance Timeline Projection Bootstrap Contract"
		);
		assertThat(readmeMarkdown).contains(
				"docs/governance-timeline-projection-bootstrap-contract.md"
		);
	}
}
