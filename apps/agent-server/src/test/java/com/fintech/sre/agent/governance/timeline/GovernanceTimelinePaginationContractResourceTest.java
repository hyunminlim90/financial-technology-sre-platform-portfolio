package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class GovernanceTimelinePaginationContractResourceTest {

	@Test
	void shouldContainTimelinePaginationContract() throws IOException {
		Path document = Path.of("docs", "governance-timeline-pagination-contract.md");

		assertThat(Files.exists(document)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);

		assertThat(markdown).contains("# Governance Timeline Pagination Contract");
		assertThat(markdown).contains("occurredAt DESC, eventId DESC");
		assertThat(markdown).contains("opaque");
		assertThat(markdown).contains("recommendation records");
		assertThat(markdown).contains("knowledge update applications");
		assertThat(markdown).contains("best-effort consistent");
		assertThat(markdown).contains("duplicate avoidance");
		assertThat(markdown).contains("degraded=true");
		assertThat(markdown).contains("failedComponents");
		assertThat(markdown).contains("WebSocket");
		assertThat(markdown).contains("customer data");
		assertThat(markdown).contains("Qdrant");
	}
}
