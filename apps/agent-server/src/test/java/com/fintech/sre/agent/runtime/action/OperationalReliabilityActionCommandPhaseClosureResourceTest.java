package com.fintech.sre.agent.runtime.action;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class OperationalReliabilityActionCommandPhaseClosureResourceTest {

	@Test
	void shouldContainOperationalActionCommandPhaseClosure()
			throws IOException {
		Path document = Path.of(
				"docs",
				"runtime-operational-action-command-phase-closure.md"
		);
		Path readme = Path.of("README.md");

		assertThat(Files.exists(document)).isTrue();
		assertThat(Files.exists(readme)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);
		String readmeMarkdown = Files.readString(readme, StandardCharsets.UTF_8);

		assertThat(markdown).contains(
				"# Runtime Operational Action Command Phase Closure"
		);
		assertThat(markdown).contains("## 1. Purpose");
		assertThat(markdown).contains("## 2. Completed Scope");
		assertThat(markdown).contains("## 3. Action Command Semantics");
		assertThat(markdown).contains("## 4. Verification Request Dependency");
		assertThat(markdown).contains("## 5. Required Action Command Conditions");
		assertThat(markdown).contains("## 6. Action Command Integration Semantics");
		assertThat(markdown).contains("## 7. Action Command Candidate Boundary");
		assertThat(markdown).contains("## 8. Payment Safety Boundary");
		assertThat(markdown).contains("## 9. Lifecycle Uncertainty Boundary");
		assertThat(markdown).contains("## 10. Runtime Invariants");
		assertThat(markdown).contains("## 11. Deferred Scope");
		assertThat(markdown).contains("## 12. Non-Goals");
		assertThat(markdown).contains("## 13. Phase Closure Summary");

		assertThat(markdown).contains("ActionCommand");
		assertThat(markdown).contains("ActionCommandEvaluator");
		assertThat(markdown).contains("ActionCommandLevel");
		assertThat(markdown).contains("ActionCommandReason");
		assertThat(markdown).contains("ActionCommandScope");
		assertThat(markdown).contains("ActionCommandIntegration");
		assertThat(markdown).contains("ActionCommandIntegrationResult");
		assertThat(markdown).contains("ActionCommandIntegrationStatus");
		assertThat(markdown).contains("ActionCommandIntegrationReason");
		assertThat(markdown).contains("ActionCommandIntegrationScope");

		assertThat(markdown).contains("ActionCommand는 실행 가능한 action command 후보를 표현하는 semantic layer이다.");
		assertThat(markdown).contains("ActionCommand는 read-only이다.");
		assertThat(markdown).contains("ActionCommand는 actual action execution이 아니다.");
		assertThat(markdown).contains("ActionCommand는 action dispatch가 아니다.");
		assertThat(markdown).contains("ActionCommand는 Kubernetes API 호출이 아니다.");
		assertThat(markdown).contains("ActionCommand는 ArgoCD Sync가 아니다.");
		assertThat(markdown).contains("ActionCommand는 Terraform/OpenTofu Apply가 아니다.");
		assertThat(markdown).contains("ActionCommand는 execution permission이 아니다.");
		assertThat(markdown).contains("ActionCommand는 VerificationRequestIntegration에 의존한다.");
		assertThat(markdown).contains("ACTION_COMMAND_READY만 action command 후보가 될 수 있다.");
		assertThat(markdown).contains("actionCommandIdentifier는 필수이다.");
		assertThat(markdown).contains("actionType은 필수이다.");
		assertThat(markdown).contains("targetLayer는 필수이다.");
		assertThat(markdown).contains("blastRadiusBoundary는 필수이다.");
		assertThat(markdown).contains("rollbackBinding은 필수이다.");
		assertThat(markdown).contains("verificationBinding은 필수이다.");
		assertThat(markdown).contains("payment safety uncertainty는 BLOCKED이다.");
		assertThat(markdown).contains("critical lifecycle risk는 BLOCKED이다.");
		assertThat(markdown).contains("ActionCommandIntegration은 action command candidate readiness 해석 계층이다.");
		assertThat(markdown).contains("ACTION_COMMAND_CANDIDATE_READY는 실제 실행 권한이 아니다.");
		assertThat(markdown).contains("ActionCommandIntegration은 action authority가 아니다.");
		assertThat(markdown).contains("ActionCommandIntegration은 dispatch authority가 아니다.");
		assertThat(markdown).contains("ActionCommandIntegration은 execution authority가 아니다.");
		assertThat(markdown).contains("portfolio knowledge source 수정 금지.");

		assertThat(markdown).contains("Action Command");
		assertThat(markdown).contains("Action Execution");
		assertThat(markdown).contains("Action Dispatch");
		assertThat(markdown).contains("Kubernetes API");
		assertThat(markdown).contains("ArgoCD Sync");
		assertThat(markdown).contains("Terraform/OpenTofu Apply");
		assertThat(markdown).contains("Execution Permission");

		assertThat(markdown).contains("Actual Action Execution");
		assertThat(markdown).contains("Action Dispatch");
		assertThat(markdown).contains("Kubernetes API Integration");
		assertThat(markdown).contains("ArgoCD Sync Integration");
		assertThat(markdown).contains("Terraform/OpenTofu Apply Integration");
		assertThat(markdown).contains("SSH / Ansible Execution");
		assertThat(markdown).contains("Execution Permission");
		assertThat(markdown).contains("Action Audit History");
		assertThat(markdown).contains("Action Rollback Workflow");
		assertThat(markdown).contains("Action Verification Workflow");

		assertThat(readmeMarkdown).contains(
				"### Runtime Operational Action Command Phase Closure"
		);
		assertThat(readmeMarkdown).contains(
				"docs/runtime-operational-action-command-phase-closure.md"
		);
	}
}
