package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class GovernanceTimelineProjectionEvolutionContractResourceTest {

	@Test
	void shouldContainTimelineProjectionEvolutionContract()
			throws IOException {
		Path document = Path.of(
				"docs",
				"governance-timeline-projection-evolution-contract.md"
		);
		Path readme = Path.of("README.md");

		assertThat(Files.exists(document)).isTrue();
		assertThat(Files.exists(readme)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);
		String readmeMarkdown = Files.readString(readme, StandardCharsets.UTF_8);

		assertThat(markdown).contains(
				"# Governance Timeline Projection Evolution Contract"
		);
		assertThat(markdown).contains("append-oriented schema evolution");
		assertThat(markdown).contains("historical audit continuity");
		assertThat(markdown).contains("stable API evolution");
		assertThat(markdown).contains("stable cursor evolution");
		assertThat(markdown).contains("breaking evolution should be minimized");
		assertThat(markdown).contains("append-oriented field evolution should be preferred");
		assertThat(markdown).contains("historical projection continuity must remain preserved");
		assertThat(markdown).contains("projection overwrite should be minimized");
		assertThat(markdown).contains("existing API contract should remain preserved whenever possible");
		assertThat(markdown).contains("frontend compatibility should remain preserved");
		assertThat(markdown).contains("runtime summary compatibility should remain preserved");
		assertThat(markdown).contains("timeline type compatibility should remain preserved");
		assertThat(markdown).contains("opaque cursor semantics remain preserved");
		assertThat(markdown).contains("cursor ordering compatibility remains preserved");
		assertThat(markdown).contains("`occurredAt DESC, eventId DESC` compatibility remains preserved");
		assertThat(markdown).contains("stable pagination compatibility remains preserved");
		assertThat(markdown).contains("cursor invalidation should be minimized");
		assertThat(markdown).contains("sanitized metadata boundary remains preserved");
		assertThat(markdown).contains("unsafe payload evolution is not allowed");
		assertThat(markdown).contains("secret, token, and password persistence is not allowed");
		assertThat(markdown).contains("raw prompt and raw response persistence is not allowed");
		assertThat(markdown).contains("historical replay and rebuild compatibility remains preserved");
		assertThat(markdown).contains("replay idempotency compatibility remains preserved");
		assertThat(markdown).contains("retention and archive compatibility remains preserved");
		assertThat(markdown).contains("operator-facing informational semantics only");
		assertThat(markdown).contains("auto-remediation semantics");
		assertThat(markdown).contains("governance action trigger semantics");
		assertThat(markdown).contains("runtime aggregation to persistent projection migration compatibility remains");
		assertThat(markdown).contains("projection schema evolution compatibility remains preserved");
		assertThat(markdown).contains("frontend and API compatibility remains preserved");
		assertThat(markdown).contains("cursor contract remains preserved");
		assertThat(markdown).contains("automatic schema migration orchestration");
		assertThat(markdown).contains("event sourcing migration");
		assertThat(markdown).contains("cross-region schema coordination");
		assertThat(markdown).contains("distributed schema lock orchestration");
		assertThat(readmeMarkdown).contains(
				"### Governance Timeline Projection Evolution Contract"
		);
		assertThat(readmeMarkdown).contains(
				"docs/governance-timeline-projection-evolution-contract.md"
		);
	}
}
