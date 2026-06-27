package com.fintech.sre.agent.runtime.execution;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class OperationalReliabilityExecutionPermissionPhaseClosureResourceTest {

	@Test
	void shouldContainOperationalExecutionPermissionPhaseClosure()
			throws IOException {
		Path document = Path.of(
				"docs",
				"runtime-operational-execution-permission-phase-closure.md"
		);
		Path readme = Path.of("README.md");

		assertThat(Files.exists(document)).isTrue();
		assertThat(Files.exists(readme)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);
		String readmeMarkdown = Files.readString(readme, StandardCharsets.UTF_8);

		assertThat(markdown).contains(
				"# Runtime Operational Execution Permission Phase Closure"
		);
		assertThat(markdown).contains("## 1. Purpose");
		assertThat(markdown).contains("## 2. Completed Scope");
		assertThat(markdown).contains("## 3. Execution Permission Semantics");
		assertThat(markdown).contains("## 4. Action Command Dependency");
		assertThat(markdown).contains(
				"## 5. Required Execution Permission Conditions"
		);
		assertThat(markdown).contains(
				"## 6. Execution Permission Integration Semantics"
		);
		assertThat(markdown).contains("## 7. Execution Permission Boundary");
		assertThat(markdown).contains("## 8. Payment Safety Boundary");
		assertThat(markdown).contains("## 9. Lifecycle Uncertainty Boundary");
		assertThat(markdown).contains("## 10. Runtime Invariants");
		assertThat(markdown).contains("## 11. Deferred Scope");
		assertThat(markdown).contains("## 12. Non-Goals");
		assertThat(markdown).contains("## 13. Phase Closure Summary");

		assertThat(markdown).contains("ExecutionPermission");
		assertThat(markdown).contains("ExecutionPermissionEvaluator");
		assertThat(markdown).contains("ExecutionPermissionLevel");
		assertThat(markdown).contains("ExecutionPermissionReason");
		assertThat(markdown).contains("ExecutionPermissionScope");
		assertThat(markdown).contains("ExecutionPermissionIntegration");
		assertThat(markdown).contains("ExecutionPermissionIntegrationResult");
		assertThat(markdown).contains("ExecutionPermissionIntegrationStatus");
		assertThat(markdown).contains("ExecutionPermissionIntegrationReason");
		assertThat(markdown).contains("ExecutionPermissionIntegrationScope");

		assertThat(markdown).contains("ExecutionPermission은 실행 권한 부여 가능 상태를 표현하는 최종 Runtime Semantic Gate이다.");
		assertThat(markdown).contains("ExecutionPermission은 read-only이다.");
		assertThat(markdown).contains("ExecutionPermission은 actual action execution이 아니다.");
		assertThat(markdown).contains("ExecutionPermission은 action dispatch가 아니다.");
		assertThat(markdown).contains("ExecutionPermission은 Kubernetes API 호출이 아니다.");
		assertThat(markdown).contains("ExecutionPermission은 kubectl 실행이 아니다.");
		assertThat(markdown).contains("ExecutionPermission은 ArgoCD Sync가 아니다.");
		assertThat(markdown).contains("ExecutionPermission은 Terraform/OpenTofu Apply가 아니다.");
		assertThat(markdown).contains("ExecutionPermission은 SSH/Ansible 실행이 아니다.");
		assertThat(markdown).contains("ExecutionPermission은 ActionCommandIntegration에 의존한다.");
		assertThat(markdown).contains("EXECUTION_PERMITTED만 execution permission 후보가 될 수 있다.");
		assertThat(markdown).contains("executionPermissionIdentifier는 필수이다.");
		assertThat(markdown).contains("executionPolicy는 필수이다.");
		assertThat(markdown).contains("operatorAuthorization은 필수이다.");
		assertThat(markdown).contains("executionGuardrail은 필수이다.");
		assertThat(markdown).contains("payment safety uncertainty는 BLOCKED이다.");
		assertThat(markdown).contains("critical lifecycle risk는 BLOCKED이다.");
		assertThat(markdown).contains("ExecutionPermissionIntegration은 execution permission readiness 해석 계층이다.");
		assertThat(markdown).contains("EXECUTION_PERMISSION_READY는 실제 실행 수행이 아니다.");
		assertThat(markdown).contains("ExecutionPermissionIntegration은 action authority가 아니다.");
		assertThat(markdown).contains("ExecutionPermissionIntegration은 dispatch authority가 아니다.");
		assertThat(markdown).contains("ExecutionPermissionIntegration은 execution engine이 아니다.");
		assertThat(markdown).contains("portfolio knowledge source 수정 금지.");

		assertThat(markdown).contains("Execution Permission");
		assertThat(markdown).contains("Action Execution");
		assertThat(markdown).contains("Action Dispatch");
		assertThat(markdown).contains("Kubernetes API");
		assertThat(markdown).contains("kubectl");
		assertThat(markdown).contains("ArgoCD Sync");
		assertThat(markdown).contains("Terraform/OpenTofu Apply");
		assertThat(markdown).contains("SSH / Ansible");
		assertThat(markdown).contains("Execution Engine");

		assertThat(markdown).contains("Actual Action Execution");
		assertThat(markdown).contains("Action Dispatch");
		assertThat(markdown).contains("Kubernetes API Integration");
		assertThat(markdown).contains("kubectl Integration");
		assertThat(markdown).contains("ArgoCD Sync Integration");
		assertThat(markdown).contains("Terraform/OpenTofu Apply Integration");
		assertThat(markdown).contains("SSH / Ansible Execution");
		assertThat(markdown).contains("Execution Engine");
		assertThat(markdown).contains("Execution Audit History");
		assertThat(markdown).contains("Execution Rollback Workflow");
		assertThat(markdown).contains("Execution Verification Workflow");

		assertThat(readmeMarkdown).contains(
				"### Runtime Operational Execution Permission Phase Closure"
		);
		assertThat(readmeMarkdown).contains(
				"docs/runtime-operational-execution-permission-phase-closure.md"
		);
	}
}
