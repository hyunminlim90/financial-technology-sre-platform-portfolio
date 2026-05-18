package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class GovernanceTimelineProjectionFailureTaxonomyContractResourceTest {

	@Test
	void shouldContainTimelineProjectionFailureTaxonomyContract()
			throws IOException {
		Path document = Path.of(
				"docs",
				"governance-timeline-projection-failure-taxonomy-contract.md"
		);
		Path readme = Path.of("README.md");

		assertThat(Files.exists(document)).isTrue();
		assertThat(Files.exists(readme)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);
		String readmeMarkdown = Files.readString(readme, StandardCharsets.UTF_8);

		assertThat(markdown).contains(
				"# Governance Timeline Projection Failure Taxonomy Contract"
		);
		assertThat(markdown).contains("operator-facing informational taxonomy");
		assertThat(markdown).contains("best-effort degraded semantics");
		assertThat(markdown).contains("partial degraded projection");
		assertThat(markdown).contains("raw exception detail must not be exposed externally");
		assertThat(markdown).contains("`projection_write_failure`");
		assertThat(markdown).contains("`projection_write_partial_failure`");
		assertThat(markdown).contains("`projection_write_degraded`");
		assertThat(markdown).contains("`projection_replay_failure`");
		assertThat(markdown).contains("`projection_replay_partial_failure`");
		assertThat(markdown).contains("`projection_replay_degraded`");
		assertThat(markdown).contains("`projection_bootstrap_failure`");
		assertThat(markdown).contains("`projection_bootstrap_partial_failure`");
		assertThat(markdown).contains("`projection_bootstrap_degraded`");
		assertThat(markdown).contains("`projection_retention_failure`");
		assertThat(markdown).contains("`projection_retention_partial_failure`");
		assertThat(markdown).contains("`projection_archive_degraded`");
		assertThat(markdown).contains("`projection_consistency_degraded`");
		assertThat(markdown).contains("`projection_ordering_drift`");
		assertThat(markdown).contains("`projection_cursor_consistency_degraded`");
		assertThat(markdown).contains("`projection_partial_availability`");
		assertThat(markdown).contains("partial degraded projection remains allowed");
		assertThat(markdown).contains("failed source isolation remains visible");
		assertThat(markdown).contains("best-effort degraded availability remains preserved");
		assertThat(markdown).contains("operator-facing informational semantics only");
		assertThat(markdown).contains("auto-remediation semantics");
		assertThat(markdown).contains("governance action trigger semantics");
		assertThat(markdown).contains("approval execution semantics");
		assertThat(markdown).contains("remediation execution semantics");
		assertThat(markdown).contains("`projection_failure_total`");
		assertThat(markdown).contains("`projection_degraded_total`");
		assertThat(markdown).contains("`projection_partial_availability_total`");
		assertThat(markdown).contains("low-cardinality discipline");
		assertThat(markdown).contains("`eventId`");
		assertThat(markdown).contains("raw exception detail");
		assertThat(markdown).contains("tag explosion");
		assertThat(markdown).contains("automatic remediation mapping");
		assertThat(markdown).contains("distributed incident orchestration");
		assertThat(markdown).contains("alert routing implementation");
		assertThat(markdown).contains("cross-system failure coordination");
		assertThat(readmeMarkdown).contains(
				"### Governance Timeline Projection Failure Taxonomy Contract"
		);
		assertThat(readmeMarkdown).contains(
				"docs/governance-timeline-projection-failure-taxonomy-contract.md"
		);
	}
}
