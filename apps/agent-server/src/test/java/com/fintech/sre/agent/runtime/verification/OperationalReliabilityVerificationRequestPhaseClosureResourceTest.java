package com.fintech.sre.agent.runtime.verification;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class OperationalReliabilityVerificationRequestPhaseClosureResourceTest {

	@Test
	void shouldContainOperationalVerificationRequestPhaseClosure()
			throws IOException {
		Path document = Path.of(
				"docs",
				"runtime-operational-verification-request-phase-closure.md"
		);
		Path readme = Path.of("README.md");

		assertThat(Files.exists(document)).isTrue();
		assertThat(Files.exists(readme)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);
		String readmeMarkdown = Files.readString(readme, StandardCharsets.UTF_8);

		assertThat(markdown).contains(
				"# Runtime Operational Verification Request Phase Closure"
		);
		assertThat(markdown).contains("## 1. Purpose");
		assertThat(markdown).contains("## 2. Completed Scope");
		assertThat(markdown).contains("## 3. Verification Request Semantics");
		assertThat(markdown).contains("## 4. Approval Decision Dependency");
		assertThat(markdown).contains(
				"## 5. Required Verification Request Conditions"
		);
		assertThat(markdown).contains(
				"## 6. Verification Request Integration Semantics"
		);
		assertThat(markdown).contains("## 7. Verification Request Readiness Boundary");
		assertThat(markdown).contains("## 8. Payment Safety Boundary");
		assertThat(markdown).contains("## 9. Lifecycle Uncertainty Boundary");
		assertThat(markdown).contains("## 10. Runtime Invariants");
		assertThat(markdown).contains("## 11. Deferred Scope");
		assertThat(markdown).contains("## 12. Non-Goals");
		assertThat(markdown).contains("## 13. Phase Closure Summary");

		assertThat(markdown).contains("VerificationRequest");
		assertThat(markdown).contains("VerificationRequestEvaluator");
		assertThat(markdown).contains("VerificationRequestLevel");
		assertThat(markdown).contains("VerificationRequestReason");
		assertThat(markdown).contains("VerificationRequestScope");
		assertThat(markdown).contains("VerificationRequestIntegration");
		assertThat(markdown).contains("VerificationRequestIntegrationResult");
		assertThat(markdown).contains("VerificationRequestIntegrationStatus");
		assertThat(markdown).contains("VerificationRequestIntegrationReason");
		assertThat(markdown).contains("VerificationRequestIntegrationScope");

		assertThat(markdown).contains("VerificationRequest는 verification 단계 진입 가능 상태 표현 계층이다.");
		assertThat(markdown).contains("VerificationRequest는 read-only이다.");
		assertThat(markdown).contains("VerificationRequest는 actual verification request가 아니다.");
		assertThat(markdown).contains("VerificationRequest는 verification workflow가 아니다.");
		assertThat(markdown).contains("VerificationRequest는 verification result가 아니다.");
		assertThat(markdown).contains("VerificationRequest는 ActionCommand가 아니다.");
		assertThat(markdown).contains("VerificationRequest는 execution permission이 아니다.");
		assertThat(markdown).contains("VerificationRequest는 ApprovalDecisionIntegration에 의존한다.");
		assertThat(markdown).contains("VERIFICATION_REQUESTABLE만 verification request 후보가 될 수 있다.");
		assertThat(markdown).contains("verificationRequestIdentifier는 필수이다.");
		assertThat(markdown).contains("verificationPolicy는 필수이다.");
		assertThat(markdown).contains("verificationEvidenceRequirement는 필수이다.");
		assertThat(markdown).contains("rollbackBinding은 필수이다.");
		assertThat(markdown).contains("payment safety uncertainty는 BLOCKED이다.");
		assertThat(markdown).contains("critical lifecycle risk는 BLOCKED이다.");
		assertThat(markdown).contains("VerificationRequestIntegration은 verification request readiness 해석 계층이다.");
		assertThat(markdown).contains("VERIFICATION_REQUEST_READY는 실제 verification request 생성이 아니다.");
		assertThat(markdown).contains("VerificationRequestIntegration은 verification authority가 아니다.");
		assertThat(markdown).contains("VerificationRequestIntegration은 action authority가 아니다.");
		assertThat(markdown).contains("VerificationRequestIntegration은 execution authority가 아니다.");
		assertThat(markdown).contains("portfolio knowledge source 수정 금지.");

		assertThat(markdown).contains("Verification Request");
		assertThat(markdown).contains("Verification Workflow");
		assertThat(markdown).contains("Verification Result");
		assertThat(markdown).contains("Verification Evidence Collection");
		assertThat(markdown).contains("ActionCommand");
		assertThat(markdown).contains("Execution Permission");

		assertThat(markdown).contains("Actual Verification Request Generation");
		assertThat(markdown).contains("Verification Workflow Implementation");
		assertThat(markdown).contains("Verification Result Model");
		assertThat(markdown).contains("Verification Evidence Collection");
		assertThat(markdown).contains("Verification Audit History");
		assertThat(markdown).contains("ActionCommand Generation");
		assertThat(markdown).contains("Execution Permission");
		assertThat(markdown).contains("SRE Console Verification UI");
		assertThat(markdown).contains("Verification Analytics");

		assertThat(readmeMarkdown).contains(
				"### Runtime Operational Verification Request Phase Closure"
		);
		assertThat(readmeMarkdown).contains(
				"docs/runtime-operational-verification-request-phase-closure.md"
		);
	}
}
