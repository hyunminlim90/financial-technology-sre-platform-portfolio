package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class GovernanceTimelineProjectionReplayContractResourceTest {

	@Test
	void shouldContainTimelineProjectionReplayContract() throws IOException {
		Path document = Path.of(
				"docs",
				"governance-timeline-projection-replay-contract.md"
		);
		Path readme = Path.of("README.md");

		assertThat(Files.exists(document)).isTrue();
		assertThat(Files.exists(readme)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);
		String readmeMarkdown = Files.readString(readme, StandardCharsets.UTF_8);

		assertThat(markdown).contains(
				"# Governance Timeline Projection Replay Contract"
		);
		assertThat(markdown).contains("projection rebuild");
		assertThat(markdown).contains("schema migration replay");
		assertThat(markdown).contains("projection corruption recovery");
		assertThat(markdown).contains("historical backfill");
		assertThat(markdown).contains("projection bootstrap");
		assertThat(markdown).contains("historical ordering compatibility");
		assertThat(markdown).contains("`occurredAt DESC, eventId DESC` compatibility");
		assertThat(markdown).contains("cursor compatibility");
		assertThat(markdown).contains("stable pagination compatibility");
		assertThat(markdown).contains("`event_id`-based replay idempotency is preserved");
		assertThat(markdown).contains("duplicate projection row creation is not allowed");
		assertThat(markdown).contains("retry-safe replay is allowed");
		assertThat(markdown).contains("sanitization remains enforced during replay");
		assertThat(markdown).contains("unsafe historical payload storage is not allowed");
		assertThat(markdown).contains("secrets, tokens, and passwords must not be stored");
		assertThat(markdown).contains("payment data and customer PII must not be stored");
		assertThat(markdown).contains("raw prompts and raw responses must not be stored");
		assertThat(markdown).contains("partial replay failure is allowed");
		assertThat(markdown).contains("best-effort replay is allowed");
		assertThat(markdown).contains("failed replay source isolation remains visible");
		assertThat(markdown).contains("raw stack trace exposure is not allowed");
		assertThat(markdown).contains("`projection_replay_total`");
		assertThat(markdown).contains("`projection_replay_failure_total`");
		assertThat(markdown).contains("`projection_replay_degraded_total`");
		assertThat(markdown).contains("low-cardinality discipline");
		assertThat(markdown).contains("Replay is a read-model rebuild mechanism only.");
		assertThat(markdown).contains("trigger governance actions");
		assertThat(markdown).contains("trigger remediation");
		assertThat(markdown).contains("execute approvals");
		assertThat(markdown).contains("mutate GitOps repositories");
		assertThat(markdown).contains("mutate Kubernetes");
		assertThat(markdown).contains("trigger ArgoCD sync");
		assertThat(markdown).contains("update RAG");
		assertThat(markdown).contains("update Qdrant");
		assertThat(markdown).contains("projection schema evolution remains allowed");
		assertThat(markdown).contains("projection rebuild remains allowed");
		assertThat(markdown).contains("frontend and API compatibility must remain stable");
		assertThat(markdown).contains("cursor contract must remain stable");
		assertThat(markdown).contains("actual replay implementation");
		assertThat(markdown).contains("scheduler implementation");
		assertThat(markdown).contains("Kafka replay pipeline");
		assertThat(markdown).contains("CDC replay");
		assertThat(markdown).contains("event sourcing migration");
		assertThat(markdown).contains("exactly-once distributed replay");
		assertThat(readmeMarkdown).contains(
				"### Governance Timeline Projection Replay Contract"
		);
		assertThat(readmeMarkdown).contains(
				"docs/governance-timeline-projection-replay-contract.md"
		);
	}
}
