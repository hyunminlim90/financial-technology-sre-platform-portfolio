package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class GovernanceTimelineRuntimeContractResourceTest {

	@Test
	void shouldContainTimelineRuntimeContract() throws IOException {
		Path document = Path.of("docs", "governance-timeline-runtime-contract.md");

		assertThat(Files.exists(document)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);

		assertThat(markdown).contains("# Governance Timeline Runtime Contract");
		assertThat(markdown).contains("NORMAL");
		assertThat(markdown).contains("DEGRADED_READ_ONLY");
		assertThat(markdown).contains("ATTENTION_REQUIRED");
		assertThat(markdown).contains("HEALTHY");
		assertThat(markdown).contains("DEGRADED_CAPABLE");
		assertThat(markdown).contains("STRICT");
		assertThat(markdown).contains("UNAVAILABLE");
		assertThat(markdown).contains("timeline:DEGRADED_CAPABLE");
		assertThat(markdown).contains("Lightweight Evaluation");
		assertThat(markdown).contains("read-only");
		assertThat(markdown).contains("cursor values");
		assertThat(markdown).contains("Micrometer gauge implementation");
	}
}
