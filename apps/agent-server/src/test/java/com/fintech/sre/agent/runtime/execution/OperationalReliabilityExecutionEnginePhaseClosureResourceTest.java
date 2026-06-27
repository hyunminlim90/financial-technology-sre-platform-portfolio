package com.fintech.sre.agent.runtime.execution;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class OperationalReliabilityExecutionEnginePhaseClosureResourceTest {

	@Test
	void shouldContainOperationalExecutionEnginePhaseClosure()
			throws IOException {
		Path document = Path.of(
				"docs",
				"runtime-operational-execution-engine-phase-closure.md"
		);
		Path readme = Path.of("README.md");

		assertThat(Files.exists(document)).isTrue();
		assertThat(Files.exists(readme)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);
		String readmeMarkdown = Files.readString(readme, StandardCharsets.UTF_8);

		assertThat(markdown).contains(
				"# Runtime Operational Execution Engine Phase Closure"
		);
		assertThat(markdown).contains("## 1. Purpose");
		assertThat(markdown).contains("## 2. Completed Scope");
		assertThat(markdown).contains("## 3. Execution Engine Semantics");
		assertThat(markdown).contains("## 4. Execution Dispatch Dependency");
		assertThat(markdown).contains(
				"## 5. Required Execution Engine Conditions"
		);
		assertThat(markdown).contains(
				"## 6. Execution Engine Integration Semantics"
		);
		assertThat(markdown).contains(
				"## 7. Execution Engine Readiness Boundary"
		);
		assertThat(markdown).contains("## 8. Payment Safety Boundary");
		assertThat(markdown).contains("## 9. Lifecycle Uncertainty Boundary");
		assertThat(markdown).contains("## 10. Runtime Invariants");
		assertThat(markdown).contains("## 11. Deferred Scope");
		assertThat(markdown).contains("## 12. Non-Goals");
		assertThat(markdown).contains("## 13. Phase Closure Summary");

		assertThat(markdown).contains("ExecutionEngine");
		assertThat(markdown).contains("ExecutionEngineEvaluator");
		assertThat(markdown).contains("ExecutionEngineLevel");
		assertThat(markdown).contains("ExecutionEngineReason");
		assertThat(markdown).contains("ExecutionEngineScope");
		assertThat(markdown).contains("ExecutionEngineIntegration");
		assertThat(markdown).contains("ExecutionEngineIntegrationResult");
		assertThat(markdown).contains("ExecutionEngineIntegrationStatus");
		assertThat(markdown).contains("ExecutionEngineIntegrationReason");
		assertThat(markdown).contains("ExecutionEngineIntegrationScope");

		assertThat(markdown).contains("ExecutionEngine는 실행 엔진 선택 가능 상태를 표현하는 Runtime Semantic Layer이다.");
		assertThat(markdown).contains("ExecutionEngine는 read-only이다.");
		assertThat(markdown).contains("ExecutionEngine는 actual action execution이 아니다.");
		assertThat(markdown).contains("ExecutionEngine는 actual dispatch가 아니다.");
		assertThat(markdown).contains("ExecutionEngine는 Kubernetes API 호출이 아니다.");
		assertThat(markdown).contains("ExecutionEngine는 kubectl 실행이 아니다.");
		assertThat(markdown).contains("ExecutionEngine는 ArgoCD Sync가 아니다.");
		assertThat(markdown).contains("ExecutionEngine는 Terraform/OpenTofu Apply가 아니다.");
		assertThat(markdown).contains("ExecutionEngine는 SSH/Ansible 실행이 아니다.");
		assertThat(markdown).contains("ExecutionEngine는 특정 Execution Engine 구현이 아니다.");
		assertThat(markdown).contains("ExecutionEngine는 ExecutionDispatchIntegration에 의존한다.");
		assertThat(markdown).contains("EXECUTION_ENGINE_READY만 execution engine 후보가 될 수 있다.");
		assertThat(markdown).contains("executionEngineIdentifier는 필수이다.");
		assertThat(markdown).contains("executionEngineType은 필수이다.");
		assertThat(markdown).contains("executionEndpointBinding은 필수이다.");
		assertThat(markdown).contains("executionPolicy는 필수이다.");
		assertThat(markdown).contains("payment safety uncertainty는 BLOCKED이다.");
		assertThat(markdown).contains("critical lifecycle risk는 BLOCKED이다.");
		assertThat(markdown).contains("ExecutionEngineIntegration은 execution engine readiness 해석 계층이다.");
		assertThat(markdown).contains("EXECUTION_ENGINE_READY_VIEW는 실제 실행 엔진 호출이 아니다.");
		assertThat(markdown).contains("ExecutionEngineIntegration은 action authority가 아니다.");
		assertThat(markdown).contains("ExecutionEngineIntegration은 dispatch authority가 아니다.");
		assertThat(markdown).contains("ExecutionEngineIntegration은 execution engine implementation이 아니다.");
		assertThat(markdown).contains("portfolio knowledge source 수정 금지.");

		assertThat(markdown).contains("Execution Engine");
		assertThat(markdown).contains("Actual Action Execution");
		assertThat(markdown).contains("Actual Dispatch");
		assertThat(markdown).contains("Kubernetes API");
		assertThat(markdown).contains("kubectl");
		assertThat(markdown).contains("ArgoCD Sync");
		assertThat(markdown).contains("Terraform/OpenTofu Apply");
		assertThat(markdown).contains("SSH / Ansible");
		assertThat(markdown).contains("Execution Engine Implementation");

		assertThat(markdown).contains("Actual Action Execution");
		assertThat(markdown).contains("Actual Dispatch");
		assertThat(markdown).contains("Kubernetes API Integration");
		assertThat(markdown).contains("kubectl Integration");
		assertThat(markdown).contains("ArgoCD Sync Integration");
		assertThat(markdown).contains("Terraform/OpenTofu Apply Integration");
		assertThat(markdown).contains("SSH / Ansible Execution");
		assertThat(markdown).contains("Execution Engine Implementation");
		assertThat(markdown).contains("Execution Engine Audit History");
		assertThat(markdown).contains("Execution Engine Rollback Workflow");
		assertThat(markdown).contains("Execution Engine Verification Workflow");

		assertThat(readmeMarkdown).contains(
				"### Runtime Operational Execution Engine Phase Closure"
		);
		assertThat(readmeMarkdown).contains(
				"docs/runtime-operational-execution-engine-phase-closure.md"
		);
	}
}
