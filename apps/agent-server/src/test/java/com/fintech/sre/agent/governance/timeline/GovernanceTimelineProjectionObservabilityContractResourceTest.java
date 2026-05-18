package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class GovernanceTimelineProjectionObservabilityContractResourceTest {

	@Test
	void shouldContainTimelineProjectionObservabilityContract()
			throws IOException {
		Path document = Path.of(
				"docs",
				"governance-timeline-projection-observability-contract.md"
		);
		Path readme = Path.of("README.md");

		assertThat(Files.exists(document)).isTrue();
		assertThat(Files.exists(readme)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);
		String readmeMarkdown = Files.readString(readme, StandardCharsets.UTF_8);

		assertThat(markdown).contains(
				"# Governance Timeline Projection Observability Contract"
		);
		assertThat(markdown).contains("projection runtime visibility");
		assertThat(markdown).contains("projection degraded visibility");
		assertThat(markdown).contains("projection replay visibility");
		assertThat(markdown).contains("projection retention visibility");
		assertThat(markdown).contains("operator-facing observability");
		assertThat(markdown).contains("projection write visibility");
		assertThat(markdown).contains("projection lag visibility");
		assertThat(markdown).contains("projection degraded state visibility");
		assertThat(markdown).contains("best-effort availability visibility");
		assertThat(markdown).contains("partial failure visibility");
		assertThat(markdown).contains("projection write failure visibility");
		assertThat(markdown).contains("replay execution visibility");
		assertThat(markdown).contains("historical rebuild visibility");
		assertThat(markdown).contains("retention execution visibility");
		assertThat(markdown).contains("archive visibility");
		assertThat(markdown).contains("partition maintenance visibility");
		assertThat(markdown).contains("`projection_write_total`");
		assertThat(markdown).contains("`projection_write_failure_total`");
		assertThat(markdown).contains("`projection_write_degraded_total`");
		assertThat(markdown).contains("`projection_replay_total`");
		assertThat(markdown).contains("`projection_replay_failure_total`");
		assertThat(markdown).contains("`projection_retention_total`");
		assertThat(markdown).contains("`projection_retention_failure_total`");
		assertThat(markdown).contains("`projection_degraded_total`");
		assertThat(markdown).contains("low-cardinality metric discipline");
		assertThat(markdown).contains("raw exception detail");
		assertThat(markdown).contains("tag explosion");
		assertThat(markdown).contains("partial degraded projection states");
		assertThat(markdown).contains("failed source isolation");
		assertThat(markdown).contains("best-effort degraded read availability");
		assertThat(markdown).contains("operator-facing informational semantics only");
		assertThat(markdown).contains("auto-remediation semantics");
		assertThat(markdown).contains("governance action trigger semantics");
		assertThat(markdown).contains("approval trigger semantics");
		assertThat(markdown).contains("remediation trigger semantics");
		assertThat(markdown).contains("runtime aggregation to projection persistence migration compatibility");
		assertThat(markdown).contains("metrics continuity should remain preserved");
		assertThat(markdown).contains("frontend and runtime compatibility should remain preserved");
		assertThat(markdown).contains("actual Grafana dashboard");
		assertThat(markdown).contains("Prometheus scrape config");
		assertThat(markdown).contains("OpenTelemetry pipeline");
		assertThat(markdown).contains("alerting implementation");
		assertThat(markdown).contains("auto-remediation");
		assertThat(markdown).contains("SRE runbook automation");
		assertThat(readmeMarkdown).contains(
				"### Governance Timeline Projection Observability Contract"
		);
		assertThat(readmeMarkdown).contains(
				"docs/governance-timeline-projection-observability-contract.md"
		);
	}
}
