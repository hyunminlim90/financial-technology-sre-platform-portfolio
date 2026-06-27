package com.fintech.sre.agent.runtime.execution;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class OperationalReliabilityExecutionDispatchPhaseClosureResourceTest {

	@Test
	void shouldContainOperationalExecutionDispatchPhaseClosure()
			throws IOException {
		Path document = Path.of(
				"docs",
				"runtime-operational-execution-dispatch-phase-closure.md"
		);
		Path readme = Path.of("README.md");

		assertThat(Files.exists(document)).isTrue();
		assertThat(Files.exists(readme)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);
		String readmeMarkdown = Files.readString(readme, StandardCharsets.UTF_8);

		assertThat(markdown).contains(
				"# Runtime Operational Execution Dispatch Phase Closure"
		);
		assertThat(markdown).contains("## 1. Purpose");
		assertThat(markdown).contains("## 2. Completed Scope");
		assertThat(markdown).contains("## 3. Execution Dispatch Semantics");
		assertThat(markdown).contains("## 4. Execution Plan Dependency");
		assertThat(markdown).contains(
				"## 5. Required Execution Dispatch Conditions"
		);
		assertThat(markdown).contains(
				"## 6. Execution Dispatch Integration Semantics"
		);
		assertThat(markdown).contains("## 7. Dispatch Readiness Boundary");
		assertThat(markdown).contains("## 8. Payment Safety Boundary");
		assertThat(markdown).contains("## 9. Lifecycle Uncertainty Boundary");
		assertThat(markdown).contains("## 10. Runtime Invariants");
		assertThat(markdown).contains("## 11. Deferred Scope");
		assertThat(markdown).contains("## 12. Non-Goals");
		assertThat(markdown).contains("## 13. Phase Closure Summary");

		assertThat(markdown).contains("ExecutionDispatch");
		assertThat(markdown).contains("ExecutionDispatchEvaluator");
		assertThat(markdown).contains("ExecutionDispatchLevel");
		assertThat(markdown).contains("ExecutionDispatchReason");
		assertThat(markdown).contains("ExecutionDispatchScope");
		assertThat(markdown).contains("ExecutionDispatchIntegration");
		assertThat(markdown).contains("ExecutionDispatchIntegrationResult");
		assertThat(markdown).contains("ExecutionDispatchIntegrationStatus");
		assertThat(markdown).contains("ExecutionDispatchIntegrationReason");
		assertThat(markdown).contains("ExecutionDispatchIntegrationScope");

		assertThat(markdown).contains("ExecutionDispatch는 Execution Plan을 Execution Engine으로 전달 가능한 상태를 표현하는 Runtime Semantic Layer이다.");
		assertThat(markdown).contains("ExecutionDispatch는 read-only이다.");
		assertThat(markdown).contains("ExecutionDispatch는 actual dispatch가 아니다.");
		assertThat(markdown).contains("ExecutionDispatch는 actual action execution이 아니다.");
		assertThat(markdown).contains("ExecutionDispatch는 Kubernetes API 호출이 아니다.");
		assertThat(markdown).contains("ExecutionDispatch는 kubectl 실행이 아니다.");
		assertThat(markdown).contains("ExecutionDispatch는 ArgoCD Sync가 아니다.");
		assertThat(markdown).contains("ExecutionDispatch는 Terraform/OpenTofu Apply가 아니다.");
		assertThat(markdown).contains("ExecutionDispatch는 SSH/Ansible 실행이 아니다.");
		assertThat(markdown).contains("ExecutionDispatch는 Execution Engine 호출이 아니다.");
		assertThat(markdown).contains("ExecutionDispatch는 ExecutionPlanIntegration에 의존한다.");
		assertThat(markdown).contains("DISPATCH_READY만 execution dispatch 후보가 될 수 있다.");
		assertThat(markdown).contains("dispatchIdentifier는 필수이다.");
		assertThat(markdown).contains("executionEndpoint는 필수이다.");
		assertThat(markdown).contains("dispatchPolicy는 필수이다.");
		assertThat(markdown).contains("dispatchGuardrail은 필수이다.");
		assertThat(markdown).contains("payment safety uncertainty는 BLOCKED이다.");
		assertThat(markdown).contains("critical lifecycle risk는 BLOCKED이다.");
		assertThat(markdown).contains("ExecutionDispatchIntegration은 dispatch readiness 해석 계층이다.");
		assertThat(markdown).contains("DISPATCH_READY_VIEW는 실제 dispatch 수행이 아니다.");
		assertThat(markdown).contains("ExecutionDispatchIntegration은 action authority가 아니다.");
		assertThat(markdown).contains("ExecutionDispatchIntegration은 dispatch authority가 아니다.");
		assertThat(markdown).contains("ExecutionDispatchIntegration은 execution engine이 아니다.");
		assertThat(markdown).contains("portfolio knowledge source 수정 금지.");

		assertThat(markdown).contains("Execution Dispatch");
		assertThat(markdown).contains("Actual Dispatch");
		assertThat(markdown).contains("Action Execution");
		assertThat(markdown).contains("Kubernetes API");
		assertThat(markdown).contains("kubectl");
		assertThat(markdown).contains("ArgoCD Sync");
		assertThat(markdown).contains("Terraform/OpenTofu Apply");
		assertThat(markdown).contains("SSH / Ansible");
		assertThat(markdown).contains("Execution Engine");

		assertThat(markdown).contains("Actual Dispatch");
		assertThat(markdown).contains("Actual Action Execution");
		assertThat(markdown).contains("Kubernetes API Integration");
		assertThat(markdown).contains("kubectl Integration");
		assertThat(markdown).contains("ArgoCD Sync Integration");
		assertThat(markdown).contains("Terraform/OpenTofu Apply Integration");
		assertThat(markdown).contains("SSH / Ansible Execution");
		assertThat(markdown).contains("Execution Engine");
		assertThat(markdown).contains("Dispatch Audit History");
		assertThat(markdown).contains("Dispatch Rollback Workflow");
		assertThat(markdown).contains("Dispatch Verification Workflow");

		assertThat(readmeMarkdown).contains(
				"### Runtime Operational Execution Dispatch Phase Closure"
		);
		assertThat(readmeMarkdown).contains(
				"docs/runtime-operational-execution-dispatch-phase-closure.md"
		);
	}
}
