package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class GovernanceTimelineAggregationContractResourceTest {

	@Test
	void shouldContainTimelineAggregationContract() throws IOException {
		Path document = Path.of("docs", "governance-timeline-aggregation-contract.md");

		assertThat(Files.exists(document)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);

		assertThat(markdown).contains("# Governance Timeline Aggregation Contract");
		assertThat(markdown).contains("projection");
		assertThat(markdown).contains("sanitization");
		assertThat(markdown).contains("merge");
		assertThat(markdown).contains("deduplication");
		assertThat(markdown).contains("eventId");
		assertThat(markdown).contains("occurredAt DESC, eventId DESC");
		assertThat(markdown).contains("failedSources");
		assertThat(markdown).contains("duplicate avoidance");
		assertThat(markdown).contains("customer data");
		assertThat(markdown).contains("WebFlux streaming");
		assertThat(markdown).contains("Qdrant");
	}
}
