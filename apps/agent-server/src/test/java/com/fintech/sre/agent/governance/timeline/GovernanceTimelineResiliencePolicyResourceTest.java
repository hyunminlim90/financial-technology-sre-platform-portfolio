package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class GovernanceTimelineResiliencePolicyResourceTest {

	@Test
	void shouldContainTimelineResiliencePolicy() throws IOException {
		Path document = Path.of("docs", "governance-timeline-resilience-policy.md");

		assertThat(Files.exists(document)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);

		assertThat(markdown).contains("# Governance Timeline Resilience Policy");
		assertThat(markdown).contains("read-only");
		assertThat(markdown).contains("partial degraded");
		assertThat(markdown).contains("fail-open-read-only");
		assertThat(markdown).contains("component-query-timeout-ms");
		assertThat(markdown).contains("remediation");
		assertThat(markdown).contains("GitOps");
		assertThat(markdown).contains("RAG");
		assertThat(markdown).contains("Qdrant");
		assertThat(markdown).contains("cursor");
		assertThat(markdown).contains("raw exception message");
	}
}
