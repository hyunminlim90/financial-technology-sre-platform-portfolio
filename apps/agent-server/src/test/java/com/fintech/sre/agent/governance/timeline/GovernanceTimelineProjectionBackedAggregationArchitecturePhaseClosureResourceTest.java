package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class GovernanceTimelineProjectionBackedAggregationArchitecturePhaseClosureResourceTest {

	@Test
	void shouldContainProjectionBackedAggregationArchitecturePhaseClosure()
			throws IOException {
		Path document = Path.of(
				"docs",
				"governance-timeline-projection-backed-aggregation-architecture-phase-closure.md"
		);
		Path readme = Path.of("README.md");

		assertThat(Files.exists(document)).isTrue();
		assertThat(Files.exists(readme)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);
		String readmeMarkdown = Files.readString(readme, StandardCharsets.UTF_8);

		assertThat(markdown).contains(
				"# Governance Timeline Projection-backed Aggregation Architecture Phase Closure"
		);
		assertThat(markdown).contains("Completed Projection-backed Architecture Scope");
		assertThat(markdown).contains("Completed Runtime, Query, and Write Semantics");
		assertThat(markdown).contains("Completed Operator-facing Semantics");
		assertThat(markdown).contains("Governance Boundary Summary");
		assertThat(markdown).contains("Remaining Future Implementation Scope");
		assertThat(markdown).contains("Explicitly Deferred Scope");
		assertThat(markdown).contains("Phase Closure Summary");
		assertThat(markdown).contains("Non-goals");
		assertThat(markdown).contains("projection record mapper");
		assertThat(markdown).contains("projection writer");
		assertThat(markdown).contains("projection store");
		assertThat(markdown).contains("in-memory projection store");
		assertThat(markdown).contains("projection-backed query adapter");
		assertThat(markdown).contains("cursor pagination semantics");
		assertThat(markdown).contains("metrics");
		assertThat(markdown).contains("health and runtime summary");
		assertThat(markdown).contains("aggregation routing skeleton");
		assertThat(markdown).contains("final consistency checklist");
		assertThat(markdown).contains("`occurredAt DESC, eventId DESC` ordering");
		assertThat(markdown).contains("`NEXT` and `PREVIOUS` cursor semantics");
		assertThat(markdown).contains("same-timestamp `eventId` tie-breaker");
		assertThat(markdown).contains("`eventType` filter support");
		assertThat(markdown).contains("inclusive `from` and `to` filter support");
		assertThat(markdown).contains("low-cardinality metrics");
		assertThat(markdown).contains("lightweight health and runtime summary semantics");
		assertThat(markdown).contains("read-only informational query behavior");
		assertThat(markdown).contains("projection-backed path is read-model only");
		assertThat(markdown).contains("projection-backed query path is read-only");
		assertThat(markdown).contains("mutation and remediation execution are prohibited");
		assertThat(markdown).contains(
				"GitOps, ArgoCD, Kubernetes, Qdrant, and RAG mutation are prohibited"
		);
		assertThat(markdown).contains("real R2DBC and PostgreSQL projection store");
		assertThat(markdown).contains("optimized DB cursor query");
		assertThat(markdown).contains("projection replay runtime");
		assertThat(markdown).contains("projection recovery runtime");
		assertThat(markdown).contains("projection retention and archive runtime");
		assertThat(markdown).contains("operational migration validation");
		assertThat(markdown).contains("canary rollout strategy");
		assertThat(markdown).contains("production activation strategy");
		assertThat(markdown).contains("`@Primary` switching");
		assertThat(markdown).contains("controller activation");
		assertThat(markdown).contains("runtime production cutover");
		assertThat(markdown).contains("automatic remediation");
		assertThat(markdown).contains("write-side mutation");
		assertThat(markdown).contains("runtime fan-out remains the production default");
		assertThat(markdown).contains("controller wiring remains unchanged");
		assertThat(markdown).contains("production activation remains deferred");
		assertThat(markdown).contains("controller wiring change");
		assertThat(markdown).contains("R2DBC repository implementation");
		assertThat(markdown).contains("PostgreSQL DDL");
		assertThat(markdown).contains("runtime cutover");
		assertThat(readmeMarkdown).contains(
				"### Governance Timeline Projection-backed Aggregation Architecture Phase Closure"
		);
		assertThat(readmeMarkdown).contains(
				"docs/governance-timeline-projection-backed-aggregation-architecture-phase-closure.md"
		);
	}
}
