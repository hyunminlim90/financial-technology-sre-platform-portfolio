package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class ReactGovernanceTimelineTypesContractResourceTest {

	@Test
	void shouldContainReactTimelineTypesContract() throws IOException {
		Path document = Path.of(
				"docs",
				"react-governance-timeline-types-contract.md"
		);

		assertThat(Files.exists(document)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);

		assertThat(markdown).contains("# React Governance Timeline Types Contract");
		assertThat(markdown).contains("export interface TimelineApiResponse");
		assertThat(markdown).contains("export interface TimelineEvent");
		assertThat(markdown).contains("export type TimelineRuntimeMode");
		assertThat(markdown).contains("export type TimelineHealthStatus");
		assertThat(markdown).contains("opaque strings");
		assertThat(markdown).contains("must not be parsed by React clients");
		assertThat(markdown).contains("Mutation action types must not be introduced");
		assertThat(markdown).contains("Approve, execute, and remediate action types are forbidden");
		assertThat(markdown).contains("read-only");
	}
}
