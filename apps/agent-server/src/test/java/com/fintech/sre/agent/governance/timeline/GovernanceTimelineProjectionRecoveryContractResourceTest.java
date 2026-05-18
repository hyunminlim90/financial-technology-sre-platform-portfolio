package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class GovernanceTimelineProjectionRecoveryContractResourceTest {

	@Test
	void shouldContainTimelineProjectionRecoveryContract()
			throws IOException {
		Path document = Path.of(
				"docs",
				"governance-timeline-projection-recovery-contract.md"
		);
		Path readme = Path.of("README.md");

		assertThat(Files.exists(document)).isTrue();
		assertThat(Files.exists(readme)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);
		String readmeMarkdown = Files.readString(readme, StandardCharsets.UTF_8);

		assertThat(markdown).contains(
				"# Governance Timeline Projection Recovery Contract"
		);
		assertThat(markdown).contains("projection corruption");
		assertThat(markdown).contains("projection lag");
		assertThat(markdown).contains("partial projection write failure");
		assertThat(markdown).contains("projection replay rebuild");
		assertThat(markdown).contains("projection bootstrap recovery");
		assertThat(markdown).contains("Recovery is a read-model recovery mechanism only.");
		assertThat(markdown).contains("trigger governance actions");
		assertThat(markdown).contains("execute approvals");
		assertThat(markdown).contains("trigger remediation");
		assertThat(markdown).contains("mutate GitOps repositories");
		assertThat(markdown).contains("mutate Kubernetes");
		assertThat(markdown).contains("trigger ArgoCD sync");
		assertThat(markdown).contains("update RAG");
		assertThat(markdown).contains("update Qdrant");
		assertThat(markdown).contains("replay-based rebuild remains allowed");
		assertThat(markdown).contains("idempotent recovery remains preserved");
		assertThat(markdown).contains("ordering compatibility remains preserved");
		assertThat(markdown).contains("cursor compatibility remains preserved");
		assertThat(markdown).contains("stable pagination compatibility remains preserved");
		assertThat(markdown).contains("best-effort degraded recovery is allowed");
		assertThat(markdown).contains("partial recovery visibility remains preserved");
		assertThat(markdown).contains("failed recovery source isolation remains preserved");
		assertThat(markdown).contains("recovery degradation visibility remains preserved");
		assertThat(markdown).contains("ordering consistency after recovery");
		assertThat(markdown).contains("`event_id` dedup consistency");
		assertThat(markdown).contains("replay consistency");
		assertThat(markdown).contains("retention compatibility");
		assertThat(markdown).contains("`projection_recovery_total`");
		assertThat(markdown).contains("`projection_recovery_failure_total`");
		assertThat(markdown).contains("`projection_recovery_degraded_total`");
		assertThat(markdown).contains("low-cardinality metric discipline");
		assertThat(markdown).contains("raw exception detail");
		assertThat(markdown).contains("tag explosion");
		assertThat(markdown).contains("operator-facing informational semantics only");
		assertThat(markdown).contains("auto-remediation semantics");
		assertThat(markdown).contains("governance action trigger semantics");
		assertThat(markdown).contains("runtime aggregation to persistent projection migration compatibility");
		assertThat(markdown).contains("projection replay and rebuild compatibility remains preserved");
		assertThat(markdown).contains("frontend and API compatibility remains preserved");
		assertThat(markdown).contains("cursor contract remains preserved");
		assertThat(markdown).contains("automatic remediation orchestration");
		assertThat(markdown).contains("distributed recovery coordinator");
		assertThat(markdown).contains("cross-region recovery");
		assertThat(markdown).contains("event sourcing migration");
		assertThat(markdown).contains("exactly-once recovery guarantee");
		assertThat(readmeMarkdown).contains(
				"### Governance Timeline Projection Recovery Contract"
		);
		assertThat(readmeMarkdown).contains(
				"docs/governance-timeline-projection-recovery-contract.md"
		);
	}
}
