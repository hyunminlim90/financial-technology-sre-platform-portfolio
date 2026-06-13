package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class OperationalReliabilityActionAdmissionReliabilityPhaseClosureResourceTest {

	@Test
	void shouldContainOperationalReliabilityActionAdmissionReliabilityPhaseClosure()
			throws IOException {
		Path document = Path.of(
				"docs",
				"runtime-operational-reliability-action-admission-reliability-phase-closure.md"
		);
		Path readme = Path.of("README.md");

		assertThat(Files.exists(document)).isTrue();
		assertThat(Files.exists(readme)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);
		String readmeMarkdown = Files.readString(readme, StandardCharsets.UTF_8);

		assertThat(markdown).contains(
				"# Runtime Operational Reliability Action Admission Reliability Phase Closure"
		);
		assertThat(markdown).contains("## 1. Purpose");
		assertThat(markdown).contains("## 2. Completed Scope");
		assertThat(markdown).contains("## 3. Action Admission Reliability Semantics");
		assertThat(markdown).contains("## 4. Verification Reliability Dependency");
		assertThat(markdown).contains(
				"## 5. Action Type / Blast Radius / Rollback / Verification / Approval Requirements"
		);
		assertThat(markdown).contains(
				"## 6. Action Admission Reliability Integration Semantics"
		);
		assertThat(markdown).contains(
				"## 7. Payment Safety / Contradiction Propagation"
		);
		assertThat(markdown).contains(
				"## 8. Operator-Facing Admission Boundary"
		);
		assertThat(markdown).contains("## 9. Runtime Invariants");
		assertThat(markdown).contains("## 10. Deferred Scope");
		assertThat(markdown).contains("## 11. Non-Goals");
		assertThat(markdown).contains("## 12. Phase Closure Summary");

		assertThat(markdown).contains("ActionAdmissionReliability");
		assertThat(markdown).contains("ActionAdmissionReliabilityEvaluator");
		assertThat(markdown).contains("ActionAdmissionReliabilityLevel");
		assertThat(markdown).contains("ActionAdmissionReliabilityReason");
		assertThat(markdown).contains("ActionAdmissionReliabilityScope");
		assertThat(markdown).contains("ActionAdmissionReliabilityIntegration");
		assertThat(markdown).contains("ActionAdmissionReliabilityIntegrationResult");
		assertThat(markdown).contains("ActionAdmissionReliabilityIntegrationStatus");
		assertThat(markdown).contains("ActionAdmissionReliabilityIntegrationReason");
		assertThat(markdown).contains("ActionAdmissionReliabilityIntegrationScope");

		assertThat(markdown).contains(
				"ActionAdmissionReliability는 VerificationReliability 위의 admission-readiness 신뢰도"
		);
		assertThat(markdown).contains("ActionAdmissionReliability는 read-only");
		assertThat(markdown).contains("ActionAdmissionReliability는 action admission mutation이 아님");
		assertThat(markdown).contains("ActionAdmissionReliability는 실제 ActionCommand 생성이 아님");
		assertThat(markdown).contains("ActionAdmissionReliability는 실제 action admission 결과가 아님");
		assertThat(markdown).contains("ActionAdmissionReliability는 execution permission이 아님");
		assertThat(markdown).contains("ActionAdmissionReliability는 approval이 아님");
		assertThat(markdown).contains("BLOCKED verification reliability → action admission BLOCKED");
		assertThat(markdown).contains("UNRELIABLE verification reliability → action admission UNRELIABLE");
		assertThat(markdown).contains("LOW verification reliability → action admission downgrade");
		assertThat(markdown).contains("missing action type → action admission BLOCKED");
		assertThat(markdown).contains("missing blast radius boundary → action admission BLOCKED");
		assertThat(markdown).contains("missing rollback binding → action admission BLOCKED");
		assertThat(markdown).contains("missing verification binding → action admission BLOCKED");
		assertThat(markdown).contains("missing human approval requirement → action admission BLOCKED");
		assertThat(markdown).contains("payment safety uncertainty → action admission downgrade");
		assertThat(markdown).contains("payment safety uncertainty → lifecycle CRITICAL risk 유지");
		assertThat(markdown).contains("contradictory verification/action admission → lifecycle uncertainty 전파");
		assertThat(markdown).contains("BLOCKED action admission reliability는 ActionCommand candidate 노출 금지");
		assertThat(markdown).contains("UNRELIABLE action admission reliability는 admission certainty 금지");
		assertThat(markdown).contains(
				"HIGH action admission reliability는 HIGH verification reliability + action type + blast radius boundary + rollback binding + verification binding + human approval required + no payment uncertainty + no contradiction 필요"
		);
		assertThat(markdown).contains("portfolio knowledge source 수정 금지");

		assertThat(markdown).contains("persisted action admission reliability history");
		assertThat(markdown).contains("action admission reliability trend analysis");
		assertThat(markdown).contains("policy-configurable action admission reliability rules");
		assertThat(markdown).contains("SRE Console admission readiness visualization");
		assertThat(markdown).contains("Actual Action Admission implementation");
		assertThat(markdown).contains("ActionCommand generation");
		assertThat(markdown).contains("Execution Permission integration");
		assertThat(markdown).contains("Diagnostic Action integration");
		assertThat(markdown).contains("Incident Closure integration");
		assertThat(markdown).contains("API authorization integration");

		assertThat(readmeMarkdown).contains(
				"### Runtime Operational Reliability Action Admission Reliability Phase Closure"
		);
		assertThat(readmeMarkdown).contains(
				"docs/runtime-operational-reliability-action-admission-reliability-phase-closure.md"
		);
	}
}
