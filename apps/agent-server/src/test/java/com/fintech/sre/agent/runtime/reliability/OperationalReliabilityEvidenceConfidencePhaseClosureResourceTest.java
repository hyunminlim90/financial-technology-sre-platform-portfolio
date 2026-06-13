package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class OperationalReliabilityEvidenceConfidencePhaseClosureResourceTest {

	@Test
	void shouldContainOperationalReliabilityEvidenceConfidencePhaseClosure()
			throws IOException {
		Path document = Path.of(
				"docs",
				"runtime-operational-reliability-evidence-confidence-phase-closure.md"
		);
		Path readme = Path.of("README.md");

		assertThat(Files.exists(document)).isTrue();
		assertThat(Files.exists(readme)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);
		String readmeMarkdown = Files.readString(readme, StandardCharsets.UTF_8);

		assertThat(markdown).contains(
				"# Runtime Operational Reliability Evidence Confidence Phase Closure"
		);
		assertThat(markdown).contains("## 1. Purpose");
		assertThat(markdown).contains("## 2. Completed Scope");
		assertThat(markdown).contains("## 3. Trust vs Confidence Boundary");
		assertThat(markdown).contains("## 4. Evidence Confidence Semantics");
		assertThat(markdown).contains("## 5. Confidence Integration Semantics");
		assertThat(markdown).contains("## 6. Payment Evidence Confidence Rule");
		assertThat(markdown).contains("## 7. Operator-Facing Confidence Boundary");
		assertThat(markdown).contains("## 8. Runtime Invariants");
		assertThat(markdown).contains("## 9. Deferred Scope");
		assertThat(markdown).contains("## 10. Non-Goals");
		assertThat(markdown).contains("## 11. Phase Closure Summary");

		assertThat(markdown).contains("EvidenceConfidence");
		assertThat(markdown).contains("EvidenceConfidenceCalculator");
		assertThat(markdown).contains("EvidenceConfidenceLevel");
		assertThat(markdown).contains("EvidenceConfidenceReason");
		assertThat(markdown).contains("EvidenceConfidenceScope");
		assertThat(markdown).contains("EvidenceConfidenceIntegration");
		assertThat(markdown).contains("EvidenceConfidenceIntegrationResult");
		assertThat(markdown).contains("EvidenceConfidenceIntegrationStatus");
		assertThat(markdown).contains("EvidenceConfidenceIntegrationReason");
		assertThat(markdown).contains("EvidenceConfidenceIntegrationScope");

		assertThat(markdown).contains("Trust != Confidence");
		assertThat(markdown).contains("confidence는 evidence sufficiency 의미론");
		assertThat(markdown).contains(
				"HIGH trust여도 evidence coverage 부족하면 LOW / INSUFFICIENT confidence 가능"
		);
		assertThat(markdown).contains("INSUFFICIENT confidence는 assessment certainty 금지");
		assertThat(markdown).contains("LOW confidence는 operator-facing warning");
		assertThat(markdown).contains("MEDIUM confidence는 partial confidence");
		assertThat(markdown).contains("HIGH confidence만 confident evidence view 후보");
		assertThat(markdown).contains("payment confidence downgrade는 payment safety uncertainty로 전파");
		assertThat(markdown).contains("contradictory evidence confidence는 risk/uncertainty로 전파");
		assertThat(markdown).contains("confidence는 read-only");
		assertThat(markdown).contains("confidence는 evidence mutation이 아님");
		assertThat(markdown).contains("숫자 score 금지");
		assertThat(markdown).contains("weighting algorithm 금지");
		assertThat(markdown).contains("ML confidence 금지");
		assertThat(markdown).contains("Bayesian confidence 금지");
		assertThat(markdown).contains("LLM confidence 금지");
		assertThat(markdown).contains("recommendation authority 없음");
		assertThat(markdown).contains("execution authority 없음");
		assertThat(markdown).contains("portfolio knowledge source 수정 금지");

		assertThat(markdown).contains("statistical confidence model");
		assertThat(markdown).contains("Bayesian confidence model");
		assertThat(markdown).contains("historical confidence trend");
		assertThat(markdown).contains("policy-configurable confidence rule");
		assertThat(markdown).contains("SRE Console confidence visualization");
		assertThat(markdown).contains("compliance/report export");

		assertThat(readmeMarkdown).contains(
				"### Runtime Operational Reliability Evidence Confidence Phase Closure"
		);
		assertThat(readmeMarkdown).contains(
				"docs/runtime-operational-reliability-evidence-confidence-phase-closure.md"
		);
	}
}
