package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class ReactGovernanceTimelinePanelContractResourceTest {

	@Test
	void shouldContainReactTimelinePanelContract() throws IOException {
		Path document = Path.of(
				"docs",
				"react-governance-timeline-panel-contract.md"
		);

		assertThat(Files.exists(document)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);

		assertThat(markdown).contains("# React Governance Timeline Panel Contract");
		assertThat(markdown).contains("/internal/governance/timeline/runtime-summary");
		assertThat(markdown).contains("/internal/governance/timeline");
		assertThat(markdown).contains("/internal/governance/timeline/health");
		assertThat(markdown).contains("Cursor Pagination UX");
		assertThat(markdown).contains("NEXT");
		assertThat(markdown).contains("PREVIOUS");
		assertThat(markdown).contains("Degraded Timeline Rendering");
		assertThat(markdown).contains("read-only");
		assertThat(markdown).contains("approve button");
		assertThat(markdown).contains("execute button");
		assertThat(markdown).contains("remediate button");
		assertThat(markdown).contains("GitOps");
		assertThat(markdown).contains("Qdrant");
	}
}
