package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class ReactGovernanceTimelineAccessibilityContractResourceTest {

	@Test
	void shouldContainReactTimelineAccessibilityContract() throws IOException {
		Path document = Path.of(
				"docs",
				"react-governance-timeline-accessibility-contract.md"
		);

		assertThat(Files.exists(document)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);

		assertThat(markdown).contains("# React Governance Timeline Accessibility Contract");
		assertThat(markdown).contains("Keyboard Navigation");
		assertThat(markdown).contains("Screen-reader Semantics");
		assertThat(markdown).contains("Loading, Error, and Degraded Announcements");
		assertThat(markdown).contains("must not be communicated by color only");
		assertThat(markdown).contains("human-readable presentation");
		assertThat(markdown).contains("machine-readable representation");
		assertThat(markdown).contains("Focus Management");
		assertThat(markdown).contains("Pagination Accessibility");
		assertThat(markdown).contains("approve control");
		assertThat(markdown).contains("GitOps action");
		assertThat(markdown).contains("Qdrant action");
		assertThat(markdown).contains("navigation-only");
	}
}
