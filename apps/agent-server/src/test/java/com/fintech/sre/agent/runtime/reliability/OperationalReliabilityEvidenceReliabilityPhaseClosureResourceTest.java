package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class OperationalReliabilityEvidenceReliabilityPhaseClosureResourceTest {

	@Test
	void shouldContainOperationalReliabilityEvidenceReliabilityPhaseClosure()
			throws IOException {
		Path document = Path.of(
				"docs",
				"runtime-operational-reliability-evidence-reliability-phase-closure.md"
		);
		Path readme = Path.of("README.md");

		assertThat(Files.exists(document)).isTrue();
		assertThat(Files.exists(readme)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);
		String readmeMarkdown = Files.readString(readme, StandardCharsets.UTF_8);

		assertThat(markdown).contains(
				"# Runtime Operational Reliability Evidence Reliability Phase Closure"
		);
		assertThat(markdown).contains("## 1. Purpose");
		assertThat(markdown).contains("## 2. Completed Scope");
		assertThat(markdown).contains("## 3. Evidence Reliability Synthesis Semantics");
		assertThat(markdown).contains(
				"## 4. Governance / Lineage / Trust / Confidence Composition"
		);
		assertThat(markdown).contains("## 5. Evidence Reliability Integration Semantics");
		assertThat(markdown).contains("## 6. Payment Evidence Reliability Rule");
		assertThat(markdown).contains("## 7. Operator-Facing Reliability Boundary");
		assertThat(markdown).contains("## 8. Runtime Invariants");
		assertThat(markdown).contains("## 9. Deferred Scope");
		assertThat(markdown).contains("## 10. Non-Goals");
		assertThat(markdown).contains("## 11. Phase Closure Summary");

		assertThat(markdown).contains("EvidenceReliability");
		assertThat(markdown).contains("EvidenceReliabilitySynthesizer");
		assertThat(markdown).contains("EvidenceReliabilityLevel");
		assertThat(markdown).contains("EvidenceReliabilityReason");
		assertThat(markdown).contains("EvidenceReliabilityScope");
		assertThat(markdown).contains("EvidenceReliabilityIntegration");
		assertThat(markdown).contains("EvidenceReliabilityIntegrationResult");
		assertThat(markdown).contains("EvidenceReliabilityIntegrationStatus");
		assertThat(markdown).contains("EvidenceReliabilityIntegrationReason");
		assertThat(markdown).contains("EvidenceReliabilityIntegrationScope");

		assertThat(markdown).contains(
				"EvidenceReliability는 Governance + Lineage + Trust + Confidence 합성 결과"
		);
		assertThat(markdown).contains("EvidenceReliability는 read-only");
		assertThat(markdown).contains("EvidenceReliability는 evidence mutation이 아님");
		assertThat(markdown).contains("BLOCKED governance → BLOCKED reliability");
		assertThat(markdown).contains("BLOCKED lineage → BLOCKED reliability");
		assertThat(markdown).contains("UNTRUSTED trust → UNRELIABLE reliability");
		assertThat(markdown).contains("INSUFFICIENT confidence → assessment certainty 금지");
		assertThat(markdown).contains("LOW reliability → operator-facing warning");
		assertThat(markdown).contains("MEDIUM reliability → partial reliability");
		assertThat(markdown).contains(
				"HIGH reliability는 allowed governance + complete lineage + high trust + high confidence 필요"
		);
		assertThat(markdown).contains(
				"payment restricted + low/insufficient confidence → payment safety uncertainty 유지"
		);
		assertThat(markdown).contains("payment safety uncertainty는 assessment/lifecycle risk로 전파");
		assertThat(markdown).contains("recommendation authority 없음");
		assertThat(markdown).contains("execution authority 없음");
		assertThat(markdown).contains("portfolio knowledge source 수정 금지");

		assertThat(markdown).contains("persistent reliability history");
		assertThat(markdown).contains("reliability trend analysis");
		assertThat(markdown).contains("policy-configurable reliability synthesis");
		assertThat(markdown).contains("SRE Console reliability visualization");
		assertThat(markdown).contains("compliance/report export");
		assertThat(markdown).contains("API authorization integration");

		assertThat(readmeMarkdown).contains(
				"### Runtime Operational Reliability Evidence Reliability Phase Closure"
		);
		assertThat(readmeMarkdown).contains(
				"docs/runtime-operational-reliability-evidence-reliability-phase-closure.md"
		);
	}
}
