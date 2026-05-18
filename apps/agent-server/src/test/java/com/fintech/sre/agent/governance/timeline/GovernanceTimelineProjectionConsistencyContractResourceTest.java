package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class GovernanceTimelineProjectionConsistencyContractResourceTest {

	@Test
	void shouldContainTimelineProjectionConsistencyContract()
			throws IOException {
		Path document = Path.of(
				"docs",
				"governance-timeline-projection-consistency-contract.md"
		);
		Path readme = Path.of("README.md");

		assertThat(Files.exists(document)).isTrue();
		assertThat(Files.exists(readme)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);
		String readmeMarkdown = Files.readString(readme, StandardCharsets.UTF_8);

		assertThat(markdown).contains(
				"# Governance Timeline Projection Consistency Contract"
		);
		assertThat(markdown).contains("stable timeline ordering");
		assertThat(markdown).contains("stable cursor semantics");
		assertThat(markdown).contains("best-effort append-only consistency");
		assertThat(markdown).contains("operator-facing consistency visibility");
		assertThat(markdown).contains("`occurredAt DESC, eventId DESC` consistency");
		assertThat(markdown).contains("stable pagination consistency");
		assertThat(markdown).contains("ordering drift");
		assertThat(markdown).contains("opaque cursor compatibility");
		assertThat(markdown).contains("cursor ordering compatibility");
		assertThat(markdown).contains("cursor replay compatibility");
		assertThat(markdown).contains("cursor drift");
		assertThat(markdown).contains("`event_id` uniqueness");
		assertThat(markdown).contains("duplicate projection visibility");
		assertThat(markdown).contains("idempotent projection compatibility");
		assertThat(markdown).contains("replay and rebuild ordering consistency");
		assertThat(markdown).contains("idempotent replay consistency");
		assertThat(markdown).contains("historical rebuild consistency");
		assertThat(markdown).contains("archive and replay coexistence consistency");
		assertThat(markdown).contains("retention-induced pagination inconsistency");
		assertThat(markdown).contains("cursor compatibility after retention");
		assertThat(markdown).contains("partial degraded projection still preserves best-effort consistency");
		assertThat(markdown).contains("failed source isolation remains visible");
		assertThat(markdown).contains("consistency degradation visibility remains available");
		assertThat(markdown).contains("operator-facing informational semantics only");
		assertThat(markdown).contains("auto-remediation semantics");
		assertThat(markdown).contains("governance action trigger semantics");
		assertThat(markdown).contains("runtime aggregation to persistent projection migration compatibility");
		assertThat(markdown).contains("frontend and API ordering compatibility must remain preserved");
		assertThat(markdown).contains("cursor contract must remain preserved");
		assertThat(markdown).contains("projection replay compatibility must remain preserved");
		assertThat(markdown).contains("strict distributed transaction guarantee");
		assertThat(markdown).contains("exactly-once global ordering");
		assertThat(markdown).contains("cross-region total ordering");
		assertThat(markdown).contains("event sourcing migration");
		assertThat(markdown).contains("distributed lock orchestration");
		assertThat(readmeMarkdown).contains(
				"### Governance Timeline Projection Consistency Contract"
		);
		assertThat(readmeMarkdown).contains(
				"docs/governance-timeline-projection-consistency-contract.md"
		);
	}
}
