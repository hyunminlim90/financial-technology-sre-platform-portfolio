package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class ReactGovernanceTimelineRenderingContractResourceTest {

	@Test
	void shouldContainReactTimelineRenderingContract() throws IOException {
		Path document = Path.of(
				"docs",
				"react-governance-timeline-rendering-contract.md"
		);

		assertThat(Files.exists(document)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);

		assertThat(markdown).contains("# React Governance Timeline Rendering Contract");
		assertThat(markdown).contains("Severity Badge Rendering");
		assertThat(markdown).contains("`INFO` -> neutral informational state");
		assertThat(markdown).contains("`WARNING` -> degraded, review, or attention-required state");
		assertThat(markdown).contains("`ERROR` -> failed, rejected, or blocked state");
		assertThat(markdown).contains("`CRITICAL` -> payment, security, or system-critical state");
		assertThat(markdown).contains("Degraded Event Rendering");
		assertThat(markdown).contains("Partial Timeline Banner Rendering");
		assertThat(markdown).contains("loading initial page");
		assertThat(markdown).contains("retry after 5xx");
		assertThat(markdown).contains("Accessibility Baseline");
		assertThat(markdown).contains("severity badge should not rely on color only");
		assertThat(markdown).contains("approve button");
		assertThat(markdown).contains("GitOps mutation control");
		assertThat(markdown).contains("Qdrant mutation control");
	}
}
