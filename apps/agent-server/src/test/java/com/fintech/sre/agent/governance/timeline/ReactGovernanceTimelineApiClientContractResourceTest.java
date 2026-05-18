package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class ReactGovernanceTimelineApiClientContractResourceTest {

	@Test
	void shouldContainReactTimelineApiClientContract() throws IOException {
		Path document = Path.of(
				"docs",
				"react-governance-timeline-api-client-contract.md"
		);

		assertThat(Files.exists(document)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);

		assertThat(markdown).contains("# React Governance Timeline API Client Contract");
		assertThat(markdown).contains("fetchTimelinePage");
		assertThat(markdown).contains("fetchTimelineHealth");
		assertThat(markdown).contains("fetchTimelineRuntimeSummary");
		assertThat(markdown).contains("export interface TimelineQueryParams");
		assertThat(markdown).contains("opaque string");
		assertThat(markdown).contains("must not be treated as thrown client errors");
		assertThat(markdown).contains("approveRecommendation(...)");
		assertThat(markdown).contains("triggerRemediation(...)");
		assertThat(markdown).contains("internal-only");
		assertThat(markdown).contains("read-only");
	}
}
