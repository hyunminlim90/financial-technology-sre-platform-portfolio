package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class ReactGovernanceTimelineInteractionContractResourceTest {

	@Test
	void shouldContainReactTimelineInteractionContract() throws IOException {
		Path document = Path.of(
				"docs",
				"react-governance-timeline-interaction-contract.md"
		);

		assertThat(Files.exists(document)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);

		assertThat(markdown).contains("# React Governance Timeline Interaction Contract");
		assertThat(markdown).contains("load older events");
		assertThat(markdown).contains("load newer events");
		assertThat(markdown).contains("apply eventType filter");
		assertThat(markdown).contains("NEXT");
		assertThat(markdown).contains("older events");
		assertThat(markdown).contains("PREVIOUS");
		assertThat(markdown).contains("newer events");
		assertThat(markdown).contains("INVALID_TIMELINE_CURSOR");
		assertThat(markdown).contains("reset pagination state");
		assertThat(markdown).contains("TIMELINE_QUERY_FAILED");
		assertThat(markdown).contains("retry allowed");
		assertThat(markdown).contains("Degraded timeline remains navigable.");
		assertThat(markdown).contains("not a blocking modal state");
		assertThat(markdown).contains("approve recommendation");
		assertThat(markdown).contains("GitOps mutation");
		assertThat(markdown).contains("Qdrant ingestion");
		assertThat(markdown).contains("keyboard-accessible pagination controls");
	}
}
