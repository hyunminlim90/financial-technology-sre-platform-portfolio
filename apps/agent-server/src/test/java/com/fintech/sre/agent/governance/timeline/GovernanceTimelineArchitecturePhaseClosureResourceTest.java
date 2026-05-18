package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class GovernanceTimelineArchitecturePhaseClosureResourceTest {

	@Test
	void shouldContainTimelineArchitecturePhaseClosure()
			throws IOException {
		Path document = Path.of(
				"docs",
				"governance-timeline-architecture-phase-closure.md"
		);
		Path readme = Path.of("README.md");

		assertThat(Files.exists(document)).isTrue();
		assertThat(Files.exists(readme)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);
		String readmeMarkdown = Files.readString(readme, StandardCharsets.UTF_8);

		assertThat(markdown).contains(
				"# Governance Timeline Architecture Phase Closure"
		);
		assertThat(markdown).contains("timeline governance architecture stabilization");
		assertThat(markdown).contains("read-only governance audit timeline establishment");
		assertThat(markdown).contains("projection architecture governance stabilization");
		assertThat(markdown).contains("timeline query API contracts completed");
		assertThat(markdown).contains("cursor pagination contracts completed");
		assertThat(markdown).contains("runtime and health contracts completed");
		assertThat(markdown).contains("metrics and error envelope contracts completed");
		assertThat(markdown).contains("timeline runtime summary contracts completed");
		assertThat(markdown).contains("timeline health contracts completed");
		assertThat(markdown).contains("degraded runtime semantics completed");
		assertThat(markdown).contains("React panel contracts completed");
		assertThat(markdown).contains("React types, client, state, rendering, interaction, and accessibility");
		assertThat(markdown).contains("implementation readiness contracts completed");
		assertThat(markdown).contains("query store contracts completed");
		assertThat(markdown).contains("projection store, schema, and writer contracts completed");
		assertThat(markdown).contains("replay, recovery, bootstrap, and retention contracts completed");
		assertThat(markdown).contains("observability, consistency, evolution, and failure taxonomy contracts");
		assertThat(markdown).contains("governance boundary and final consistency contracts completed");
		assertThat(markdown).contains("read-only semantics");
		assertThat(markdown).contains("append-only audit continuity");
		assertThat(markdown).contains("operator-facing informational semantics");
		assertThat(markdown).contains("best-effort degraded semantics");
		assertThat(markdown).contains("mutation prohibition boundary");
		assertThat(markdown).contains("future R2DBC persistence");
		assertThat(markdown).contains("future PostgreSQL projection store");
		assertThat(markdown).contains("future React implementation");
		assertThat(markdown).contains("future projection runtime implementation");
		assertThat(markdown).contains("future observability implementation");
		assertThat(markdown).contains("execution orchestration deferred");
		assertThat(markdown).contains("decision automation deferred");
		assertThat(markdown).contains("autonomous remediation deferred");
		assertThat(markdown).contains("distributed governance runtime deferred");
		assertThat(markdown).contains("AI auto-execution deferred");
		assertThat(markdown).contains("architecture contract phase is completed");
		assertThat(markdown).contains("Future implementation must preserve the established governance boundaries");
		assertThat(markdown).contains("autonomous governance execution");
		assertThat(markdown).contains("remediation orchestration");
		assertThat(markdown).contains("distributed workflow engine");
		assertThat(markdown).contains("execution automation");
		assertThat(readmeMarkdown).contains(
				"### Governance Timeline Architecture Phase Closure"
		);
		assertThat(readmeMarkdown).contains(
				"docs/governance-timeline-architecture-phase-closure.md"
		);
	}
}
