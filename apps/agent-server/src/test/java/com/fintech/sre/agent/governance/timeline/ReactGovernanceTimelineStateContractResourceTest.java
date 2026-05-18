package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class ReactGovernanceTimelineStateContractResourceTest {

	@Test
	void shouldContainReactTimelineStateContract() throws IOException {
		Path document = Path.of(
				"docs",
				"react-governance-timeline-state-contract.md"
		);

		assertThat(Files.exists(document)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);

		assertThat(markdown).contains("# React Governance Timeline State Contract");
		assertThat(markdown).contains("export interface TimelinePanelState");
		assertThat(markdown).contains("export interface TimelineErrorState");
		assertThat(markdown).contains("export interface TimelinePaginationState");
		assertThat(markdown).contains("partial degraded timeline");
		assertThat(markdown).contains("INVALID_TIMELINE_CURSOR");
		assertThat(markdown).contains("cursor reset and page reload");
		assertThat(markdown).contains("TIMELINE_QUERY_FAILED");
		assertThat(markdown).contains("retry candidate");
		assertThat(markdown).contains("INVALID_TIMELINE_QUERY");
		assertThat(markdown).contains("not a retry candidate");
		assertThat(markdown).contains("approve state");
		assertThat(markdown).contains("GitOps mutation state");
		assertThat(markdown).contains("Qdrant mutation state");
	}
}
