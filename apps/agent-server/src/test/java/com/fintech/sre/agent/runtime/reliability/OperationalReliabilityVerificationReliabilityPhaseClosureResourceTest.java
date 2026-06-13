package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class OperationalReliabilityVerificationReliabilityPhaseClosureResourceTest {

	@Test
	void shouldContainOperationalReliabilityVerificationReliabilityPhaseClosure()
			throws IOException {
		Path document = Path.of(
				"docs",
				"runtime-operational-reliability-verification-reliability-phase-closure.md"
		);
		Path readme = Path.of("README.md");

		assertThat(Files.exists(document)).isTrue();
		assertThat(Files.exists(readme)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);
		String readmeMarkdown = Files.readString(readme, StandardCharsets.UTF_8);

		assertThat(markdown).contains(
				"# Runtime Operational Reliability Verification Reliability Phase Closure"
		);
		assertThat(markdown).contains("## 1. Purpose");
		assertThat(markdown).contains("## 2. Completed Scope");
		assertThat(markdown).contains("## 3. Verification Reliability Semantics");
		assertThat(markdown).contains("## 4. Approval Reliability Dependency");
		assertThat(markdown).contains(
				"## 5. Verification Binding / Evidence / Rollback Requirements"
		);
		assertThat(markdown).contains(
				"## 6. Verification Reliability Integration Semantics"
		);
		assertThat(markdown).contains(
				"## 7. Payment Safety / Contradiction Propagation"
		);
		assertThat(markdown).contains(
				"## 8. Operator-Facing Verification Boundary"
		);
		assertThat(markdown).contains("## 9. Runtime Invariants");
		assertThat(markdown).contains("## 10. Deferred Scope");
		assertThat(markdown).contains("## 11. Non-Goals");
		assertThat(markdown).contains("## 12. Phase Closure Summary");

		assertThat(markdown).contains("VerificationReliability");
		assertThat(markdown).contains("VerificationReliabilityEvaluator");
		assertThat(markdown).contains("VerificationReliabilityLevel");
		assertThat(markdown).contains("VerificationReliabilityReason");
		assertThat(markdown).contains("VerificationReliabilityScope");
		assertThat(markdown).contains("VerificationReliabilityIntegration");
		assertThat(markdown).contains("VerificationReliabilityIntegrationResult");
		assertThat(markdown).contains("VerificationReliabilityIntegrationStatus");
		assertThat(markdown).contains("VerificationReliabilityIntegrationReason");
		assertThat(markdown).contains("VerificationReliabilityIntegrationScope");

		assertThat(markdown).contains(
				"VerificationReliability는 ApprovalReliability 위의 verification-readiness 신뢰도"
		);
		assertThat(markdown).contains("VerificationReliability는 read-only");
		assertThat(markdown).contains("VerificationReliability는 verification mutation이 아님");
		assertThat(markdown).contains("VerificationReliability는 실제 verification 실행이 아님");
		assertThat(markdown).contains("VerificationReliability는 verification request 생성이 아님");
		assertThat(markdown).contains("VerificationReliability는 verification workflow 구현이 아님");
		assertThat(markdown).contains("VerificationReliability는 verification report 생성이 아님");
		assertThat(markdown).contains("VerificationReliability는 execution permission이 아님");
		assertThat(markdown).contains("VerificationReliability는 ActionCommand admission이 아님");
		assertThat(markdown).contains("BLOCKED approval reliability → verification BLOCKED");
		assertThat(markdown).contains("UNRELIABLE approval reliability → verification UNRELIABLE");
		assertThat(markdown).contains("LOW approval reliability → verification downgrade");
		assertThat(markdown).contains("missing verification binding → verification BLOCKED");
		assertThat(markdown).contains("missing verification evidence requirement → verification BLOCKED");
		assertThat(markdown).contains("missing rollback binding → verification BLOCKED");
		assertThat(markdown).contains("payment safety uncertainty → verification downgrade");
		assertThat(markdown).contains("payment safety uncertainty → lifecycle CRITICAL risk 유지");
		assertThat(markdown).contains("contradictory approval/recommendation/verification → lifecycle uncertainty 전파");
		assertThat(markdown).contains("BLOCKED verification reliability는 verification request 금지");
		assertThat(markdown).contains("UNRELIABLE verification reliability는 verification certainty 금지");
		assertThat(markdown).contains(
				"HIGH verification reliability는 HIGH approval reliability + verification binding + verification evidence requirement + rollback binding + no payment uncertainty + no contradiction 필요"
		);
		assertThat(markdown).contains("portfolio knowledge source 수정 금지");

		assertThat(markdown).contains("persisted verification reliability history");
		assertThat(markdown).contains("verification reliability trend analysis");
		assertThat(markdown).contains("policy-configurable verification reliability rules");
		assertThat(markdown).contains("SRE Console verification readiness visualization");
		assertThat(markdown).contains("Actual Verification Workflow integration");
		assertThat(markdown).contains("Verification Report generation");
		assertThat(markdown).contains("Action Admission integration");
		assertThat(markdown).contains("Incident Closure integration");
		assertThat(markdown).contains("API authorization integration");

		assertThat(readmeMarkdown).contains(
				"### Runtime Operational Reliability Verification Reliability Phase Closure"
		);
		assertThat(readmeMarkdown).contains(
				"docs/runtime-operational-reliability-verification-reliability-phase-closure.md"
		);
	}
}
