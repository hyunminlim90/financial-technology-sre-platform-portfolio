package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class GovernanceTimelineProjectionRetentionContractResourceTest {

	@Test
	void shouldContainTimelineProjectionRetentionContract() throws IOException {
		Path document = Path.of(
				"docs",
				"governance-timeline-projection-retention-contract.md"
		);
		Path readme = Path.of("README.md");

		assertThat(Files.exists(document)).isTrue();
		assertThat(Files.exists(readme)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);
		String readmeMarkdown = Files.readString(readme, StandardCharsets.UTF_8);

		assertThat(markdown).contains(
				"# Governance Timeline Projection Retention Contract"
		);
		assertThat(markdown).contains("projection growth management");
		assertThat(markdown).contains("query performance preservation");
		assertThat(markdown).contains("cold archive compatibility");
		assertThat(markdown).contains("long-term audit scalability");
		assertThat(markdown).contains("read-model maintenance boundaries only");
		assertThat(markdown).contains("retention does not imply deletion of source governance records");
		assertThat(markdown).contains("cold archive remains allowed");
		assertThat(markdown).contains("historical replay compatibility remains preserved");
		assertThat(markdown).contains("historical audit continuity remains preserved");
		assertThat(markdown).contains("`occurred_at`-based partitioning");
		assertThat(markdown).contains("append-only compatible partition layout");
		assertThat(markdown).contains("ordering-compatible partition strategies");
		assertThat(markdown).contains("Retention is a read-model maintenance mechanism only.");
		assertThat(markdown).contains("trigger governance actions");
		assertThat(markdown).contains("trigger remediation");
		assertThat(markdown).contains("execute approvals");
		assertThat(markdown).contains("overwrite historical audit state");
		assertThat(markdown).contains("Projection mutation should remain minimal");
		assertThat(markdown).contains("active projection range `occurredAt DESC, eventId DESC` ordering");
		assertThat(markdown).contains("cursor stability");
		assertThat(markdown).contains("stable pagination compatibility");
		assertThat(markdown).contains("archive and replay may coexist");
		assertThat(markdown).contains("historical rebuild compatibility remains preserved");
		assertThat(markdown).contains("projection replay compatibility remains preserved");
		assertThat(markdown).contains("`projection_retention_total`");
		assertThat(markdown).contains("`projection_archive_total`");
		assertThat(markdown).contains("`projection_retention_failure_total`");
		assertThat(markdown).contains("low-cardinality discipline");
		assertThat(markdown).contains("raw archive path detail");
		assertThat(markdown).contains("runtime aggregation may migrate to persistent projection storage");
		assertThat(markdown).contains("archive storage evolution remains allowed");
		assertThat(markdown).contains("frontend and API compatibility must remain stable");
		assertThat(markdown).contains("cursor contract must remain stable");
		assertThat(markdown).contains("actual retention scheduler");
		assertThat(markdown).contains("partition DDL");
		assertThat(markdown).contains("archive storage implementation");
		assertThat(markdown).contains("S3 integration");
		assertThat(markdown).contains("tiered storage implementation");
		assertThat(markdown).contains("automatic legal retention policy");
		assertThat(readmeMarkdown).contains(
				"### Governance Timeline Projection Retention Contract"
		);
		assertThat(readmeMarkdown).contains(
				"docs/governance-timeline-projection-retention-contract.md"
		);
	}
}
