package com.fintech.sre.agent.governance.detail;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class GovernanceDetailQueryContractResourceTest {

	@Test
	void shouldContainReadOnlyInternalOnlyDetailContract() throws IOException {
		Path document = Path.of("docs", "governance-detail-query-contract.md");

		assertThat(Files.exists(document)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);

		assertThat(markdown).contains("# Governance Detail Query Contract");
		assertThat(markdown).contains("Incident Detail Contract");
		assertThat(markdown).contains("Recommendation Detail Contract");
		assertThat(markdown).contains("Learning Detail Contract");
		assertThat(markdown).contains("Knowledge Update Detail Contract");
		assertThat(markdown).contains("Timeline Contract");
		assertThat(markdown).contains("read-only");
		assertThat(markdown).contains("internal-only");
		assertThat(markdown).contains("must not");
		assertThat(markdown).contains("secret");
		assertThat(markdown).contains("token");
		assertThat(markdown).contains("payload");
		assertThat(markdown).contains("rawLog");
		assertThat(markdown).contains("Qdrant");
	}
}
