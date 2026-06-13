package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class OperationalReliabilityEvidenceGovernancePhaseClosureResourceTest {

	@Test
	void shouldContainOperationalReliabilityEvidenceGovernancePhaseClosure()
			throws IOException {
		Path document = Path.of(
				"docs",
				"runtime-operational-reliability-evidence-governance-phase-closure.md"
		);
		Path readme = Path.of("README.md");

		assertThat(Files.exists(document)).isTrue();
		assertThat(Files.exists(readme)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);
		String readmeMarkdown = Files.readString(readme, StandardCharsets.UTF_8);

		assertThat(markdown).contains(
				"# Runtime Operational Reliability Evidence Governance Phase Closure"
		);
		assertThat(markdown).contains("## 1. Purpose");
		assertThat(markdown).contains("## 2. Completed Scope");
		assertThat(markdown).contains("## 3. Evidence Governance Policy Semantics");
		assertThat(markdown).contains(
				"## 4. Evidence Provenance / Trust / Integrity / Classification"
		);
		assertThat(markdown).contains("## 5. Governance Integration Semantics");
		assertThat(markdown).contains("## 6. Operator-Facing Exposure Boundary");
		assertThat(markdown).contains("## 7. Payment Safety Evidence Governance");
		assertThat(markdown).contains("## 8. Runtime Invariants");
		assertThat(markdown).contains("## 9. Deferred Scope");
		assertThat(markdown).contains("## 10. Non-Goals");
		assertThat(markdown).contains("## 11. Phase Closure Summary");

		assertThat(markdown).contains("EvidenceGovernancePolicy");
		assertThat(markdown).contains("EvidenceTrustLevel");
		assertThat(markdown).contains("EvidenceIntegrityStatus");
		assertThat(markdown).contains("EvidenceProvenance");
		assertThat(markdown).contains("EvidenceClassification");
		assertThat(markdown).contains("EvidenceGovernanceIntegration");
		assertThat(markdown).contains("EvidenceGovernanceIntegrationResult");
		assertThat(markdown).contains("EvidenceGovernanceIntegrationStatus");
		assertThat(markdown).contains("EvidenceGovernanceIntegrationReason");
		assertThat(markdown).contains("EvidenceGovernanceIntegrationScope");

		assertThat(markdown).contains("evidence governance는 policy/read-model only");
		assertThat(markdown).contains("governance는 evidence mutation을 수행하지 않음");
		assertThat(markdown).contains("governance는 recommendation authority가 아님");
		assertThat(markdown).contains("governance는 execution authority가 아님");
		assertThat(markdown).contains("UNKNOWN provenance는 trust downgrade");
		assertThat(markdown).contains("MISSING provenance는 untrusted");
		assertThat(markdown).contains(
				"CONTRADICTORY evidence는 integrity degraded/contradictory"
		);
		assertThat(markdown).contains("BLOCKED evidence는 API response 노출 금지");
		assertThat(markdown).contains(
				"GOVERNANCE_PROTECTED evidence는 operator-facing 제한"
		);
		assertThat(markdown).contains(
				"payment RESTRICTED evidence는 payment safety state에 반영"
		);
		assertThat(markdown).contains("sanitized evidence만 API boundary 통과 가능");
		assertThat(markdown).contains(
				"raw payload / credential / secret / token / internal IP 포함 evidence는 보호 또는 차단"
		);
		assertThat(markdown).contains("portfolio knowledge source 수정 금지");

		assertThat(markdown).contains("actual redaction engine");
		assertThat(markdown).contains("persistent governance audit store");
		assertThat(markdown).contains("policy configuration");
		assertThat(markdown).contains("API authorization integration");
		assertThat(markdown).contains("streaming governance events");
		assertThat(markdown).contains("production data retention policy");
		assertThat(markdown).contains("evidence lineage storage");
		assertThat(markdown).contains("compliance export workflow");

		assertThat(readmeMarkdown).contains(
				"### Runtime Operational Reliability Evidence Governance Phase Closure"
		);
		assertThat(readmeMarkdown).contains(
				"docs/runtime-operational-reliability-evidence-governance-phase-closure.md"
		);
	}
}
