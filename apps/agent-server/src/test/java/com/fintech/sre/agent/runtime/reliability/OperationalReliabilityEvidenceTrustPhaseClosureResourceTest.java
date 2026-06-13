package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class OperationalReliabilityEvidenceTrustPhaseClosureResourceTest {

	@Test
	void shouldContainOperationalReliabilityEvidenceTrustPhaseClosure()
			throws IOException {
		Path document = Path.of(
				"docs",
				"runtime-operational-reliability-evidence-trust-phase-closure.md"
		);
		Path readme = Path.of("README.md");

		assertThat(Files.exists(document)).isTrue();
		assertThat(Files.exists(readme)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);
		String readmeMarkdown = Files.readString(readme, StandardCharsets.UTF_8);

		assertThat(markdown).contains(
				"# Runtime Operational Reliability Evidence Trust Phase Closure"
		);
		assertThat(markdown).contains("## 1. Purpose");
		assertThat(markdown).contains("## 2. Completed Scope");
		assertThat(markdown).contains("## 3. Evidence Trust Score Semantics");
		assertThat(markdown).contains("## 4. Trust Integration Semantics");
		assertThat(markdown).contains("## 5. Operator-Facing Trust Boundary");
		assertThat(markdown).contains("## 6. Payment Evidence Trust Rule");
		assertThat(markdown).contains("## 7. Runtime Invariants");
		assertThat(markdown).contains("## 8. Deferred Scope");
		assertThat(markdown).contains("## 9. Non-Goals");
		assertThat(markdown).contains("## 10. Phase Closure Summary");

		assertThat(markdown).contains("EvidenceTrustScore");
		assertThat(markdown).contains("EvidenceTrustScoreCalculator");
		assertThat(markdown).contains("EvidenceTrustScoreLevel");
		assertThat(markdown).contains("EvidenceTrustScoreReason");
		assertThat(markdown).contains("EvidenceTrustScoreScope");
		assertThat(markdown).contains("EvidenceTrustIntegration");
		assertThat(markdown).contains("EvidenceTrustIntegrationResult");
		assertThat(markdown).contains("EvidenceTrustIntegrationStatus");
		assertThat(markdown).contains("EvidenceTrustIntegrationReason");
		assertThat(markdown).contains("EvidenceTrustIntegrationScope");

		assertThat(markdown).contains("trust score는 숫자 기반이 아님");
		assertThat(markdown).contains("trust score는 ML 기반이 아님");
		assertThat(markdown).contains("trust score는 deterministic semantic level");
		assertThat(markdown).contains(
				"HIGH / MEDIUM / LOW / UNTRUSTED / UNKNOWN 의미 고정"
		);
		assertThat(markdown).contains("UNTRUSTED는 trusted summary 불가");
		assertThat(markdown).contains("LOW는 operator-facing warning");
		assertThat(markdown).contains("MEDIUM은 partial trust");
		assertThat(markdown).contains("HIGH만 trusted evidence view 후보");
		assertThat(markdown).contains("blocked evidence는 API response 노출 금지");
		assertThat(markdown).contains("payment restricted evidence는 trust restriction 유지");
		assertThat(markdown).contains("trust integration은 evidence mutation이 아님");
		assertThat(markdown).contains("recommendation authority 없음");
		assertThat(markdown).contains("execution authority 없음");
		assertThat(markdown).contains("portfolio knowledge source 수정 금지");

		assertThat(markdown).contains("persistent trust history");
		assertThat(markdown).contains("trust trend analysis");
		assertThat(markdown).contains("policy-configurable scoring");
		assertThat(markdown).contains("SRE Console trust visualization");
		assertThat(markdown).contains("compliance/report export");
		assertThat(markdown).contains("API authorization integration");

		assertThat(readmeMarkdown).contains(
				"### Runtime Operational Reliability Evidence Trust Phase Closure"
		);
		assertThat(readmeMarkdown).contains(
				"docs/runtime-operational-reliability-evidence-trust-phase-closure.md"
		);
	}
}
