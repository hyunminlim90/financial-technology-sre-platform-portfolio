package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class GovernanceTimelineMappingContractResourceTest {

	@Test
	void shouldContainTimelineMappingContract() throws IOException {
		Path document = Path.of("docs", "governance-timeline-mapping-contract.md");

		assertThat(Files.exists(document)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);

		assertThat(markdown).contains("# Governance Timeline Mapping Contract");
		assertThat(markdown).contains("RecommendationRecord");
		assertThat(markdown).contains("APPROVAL_DECIDED");
		assertThat(markdown).contains("Actor Mapping");
		assertThat(markdown).contains("Resource Mapping");
		assertThat(markdown).contains("Severity Mapping");
		assertThat(markdown).contains("deterministic");
		assertThat(markdown).contains("{sourceType}:{sourceId}");
		assertThat(markdown).contains("sanitize");
		assertThat(markdown).contains("customer data");
		assertThat(markdown).contains("WebFlux streaming");
		assertThat(markdown).contains("Qdrant");
	}
}
