package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class OperationalReliabilityEvidenceLineagePhaseClosureResourceTest {

	@Test
	void shouldContainOperationalReliabilityEvidenceLineagePhaseClosure()
			throws IOException {
		Path document = Path.of(
				"docs",
				"runtime-operational-reliability-evidence-lineage-phase-closure.md"
		);
		Path readme = Path.of("README.md");

		assertThat(Files.exists(document)).isTrue();
		assertThat(Files.exists(readme)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);
		String readmeMarkdown = Files.readString(readme, StandardCharsets.UTF_8);

		assertThat(markdown).contains(
				"# Runtime Operational Reliability Evidence Lineage Phase Closure"
		);
		assertThat(markdown).contains("## 1. Purpose");
		assertThat(markdown).contains("## 2. Completed Scope");
		assertThat(markdown).contains("## 3. Evidence Lineage Semantics");
		assertThat(markdown).contains("## 4. Lineage Node / Edge Model");
		assertThat(markdown).contains("## 5. Lineage Integration Semantics");
		assertThat(markdown).contains("## 6. Traceability Boundary");
		assertThat(markdown).contains("## 7. Payment Evidence Lineage Rule");
		assertThat(markdown).contains("## 8. Runtime Invariants");
		assertThat(markdown).contains("## 9. Deferred Scope");
		assertThat(markdown).contains("## 10. Non-Goals");
		assertThat(markdown).contains("## 11. Phase Closure Summary");

		assertThat(markdown).contains("EvidenceLineage");
		assertThat(markdown).contains("EvidenceLineageNode");
		assertThat(markdown).contains("EvidenceLineageEdge");
		assertThat(markdown).contains("EvidenceLineageStatus");
		assertThat(markdown).contains("EvidenceLineageReason");
		assertThat(markdown).contains("EvidenceLineageIntegration");
		assertThat(markdown).contains("EvidenceLineageIntegrationResult");
		assertThat(markdown).contains("EvidenceLineageIntegrationStatus");
		assertThat(markdown).contains("EvidenceLineageIntegrationReason");
		assertThat(markdown).contains("EvidenceLineageIntegrationScope");

		assertThat(markdown).contains("lineage는 read-only traceability model");
		assertThat(markdown).contains("lineage는 evidence mutation이 아님");
		assertThat(markdown).contains(
				"source → adapter → routing → dispatch → execution → collection → assessment → summary 추적"
		);
		assertThat(markdown).contains("missing provenance는 INCOMPLETE lineage");
		assertThat(markdown).contains("INCOMPLETE lineage는 trusted summary 불가");
		assertThat(markdown).contains("BLOCKED lineage는 API response 노출 금지");
		assertThat(markdown).contains("RESTRICTED lineage는 operator-facing 제한");
		assertThat(markdown).contains("contradictory lineage risk는 uncertainty/risk로 전파");
		assertThat(markdown).contains(
				"payment lineage는 restricted/payment safety state로 전파"
		);
		assertThat(markdown).contains("lineage는 recommendation authority가 아님");
		assertThat(markdown).contains("lineage는 execution authority가 아님");
		assertThat(markdown).contains("portfolio knowledge source 수정 금지");

		assertThat(markdown).contains("graph DB storage");
		assertThat(markdown).contains("persistent lineage store");
		assertThat(markdown).contains("event-sourced lineage reconstruction");
		assertThat(markdown).contains("API exposure");
		assertThat(markdown).contains("SRE Console graph view");
		assertThat(markdown).contains("lineage query language");
		assertThat(markdown).contains("lineage retention policy");
		assertThat(markdown).contains("compliance export");

		assertThat(readmeMarkdown).contains(
				"### Runtime Operational Reliability Evidence Lineage Phase Closure"
		);
		assertThat(readmeMarkdown).contains(
				"docs/runtime-operational-reliability-evidence-lineage-phase-closure.md"
		);
	}
}
