package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class GovernanceTimelineAggregationRoutingContractResourceTest {

	@Test
	void shouldContainTimelineAggregationRoutingContract() throws IOException {
		Path document = Path.of(
				"docs",
				"governance-timeline-aggregation-routing-contract.md"
		);
		Path readme = Path.of("README.md");

		assertThat(Files.exists(document)).isTrue();
		assertThat(Files.exists(readme)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);
		String readmeMarkdown = Files.readString(readme, StandardCharsets.UTF_8);

		assertThat(markdown).contains(
				"# Governance Timeline Aggregation Routing Contract"
		);
		assertThat(markdown).contains("default aggregation mode remains `RUNTIME_FAN_OUT`");
		assertThat(markdown).contains("`PROJECTION_BACKED` is a future aggregation mode");
		assertThat(markdown).contains("activated only through explicit configuration");
		assertThat(markdown).contains("projection store must be sufficiently bootstrapped");
		assertThat(markdown).contains("projection query adapter must support cursor, filter, and from-to semantics");
		assertThat(markdown).contains("metrics, health, and runtime surfaces must expose mode differences");
		assertThat(markdown).contains("projection-backed integration testing must pass before activation");
		assertThat(markdown).contains("Rollback to `RUNTIME_FAN_OUT` must remain possible.");
		assertThat(markdown).contains("rollback must not change the API response contract");
		assertThat(markdown).contains("frontend contract must remain preserved");
		assertThat(markdown).contains("aggregation mode visibility remains available");
		assertThat(markdown).contains("runtime fan-out versus projection-backed query behavior");
		assertThat(markdown).contains("low-cardinality metric tags are allowed");
		assertThat(markdown).contains("cursor values");
		assertThat(markdown).contains("`eventId`");
		assertThat(markdown).contains("raw exception detail");
		assertThat(markdown).contains("controller API contract stability");
		assertThat(markdown).contains("`occurredAt DESC, eventId DESC` ordering compatibility");
		assertThat(markdown).contains("opaque cursor semantics");
		assertThat(markdown).contains("degraded response semantics");
		assertThat(markdown).contains("actual bean wiring switch");
		assertThat(markdown).contains("`@Primary` activation");
		assertThat(markdown).contains("runtime switch endpoint");
		assertThat(markdown).contains("projection-backed mode activation");
		assertThat(readmeMarkdown).contains(
				"### Governance Timeline Aggregation Routing Contract"
		);
		assertThat(readmeMarkdown).contains(
				"docs/governance-timeline-aggregation-routing-contract.md"
		);
	}
}
