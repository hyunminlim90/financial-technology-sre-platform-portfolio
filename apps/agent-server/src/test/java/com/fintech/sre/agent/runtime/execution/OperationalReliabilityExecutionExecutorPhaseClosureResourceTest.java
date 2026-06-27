package com.fintech.sre.agent.runtime.execution;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class OperationalReliabilityExecutionExecutorPhaseClosureResourceTest {

	@Test
	void shouldContainOperationalExecutionExecutorPhaseClosure()
			throws IOException {
		Path document = Path.of(
				"docs",
				"runtime-operational-execution-executor-phase-closure.md"
		);
		Path readme = Path.of("README.md");

		assertThat(Files.exists(document)).isTrue();
		assertThat(Files.exists(readme)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);
		String readmeMarkdown = Files.readString(readme, StandardCharsets.UTF_8);

		assertThat(markdown).contains(
				"# Runtime Operational Execution Executor Phase Closure"
		);
		assertThat(markdown).contains("## 1. Purpose");
		assertThat(markdown).contains("## 2. Completed Scope");
		assertThat(markdown).contains("## 3. Execution Executor Semantics");
		assertThat(markdown).contains("## 4. Execution Adapter Dependency");
		assertThat(markdown).contains(
				"## 5. Required Execution Executor Conditions"
		);
		assertThat(markdown).contains(
				"## 6. Execution Executor Integration Semantics"
		);
		assertThat(markdown).contains("## 7. Executor Readiness Boundary");
		assertThat(markdown).contains("## 8. Payment Safety Boundary");
		assertThat(markdown).contains("## 9. Lifecycle Uncertainty Boundary");
		assertThat(markdown).contains("## 10. Runtime Invariants");
		assertThat(markdown).contains("## 11. Deferred Scope");
		assertThat(markdown).contains("## 12. Non-Goals");
		assertThat(markdown).contains("## 13. Phase Closure Summary");

		assertThat(markdown).contains("ExecutionExecutor");
		assertThat(markdown).contains("ExecutionExecutorEvaluator");
		assertThat(markdown).contains("ExecutionExecutorLevel");
		assertThat(markdown).contains("ExecutionExecutorReason");
		assertThat(markdown).contains("ExecutionExecutorScope");
		assertThat(markdown).contains("ExecutionExecutorIntegration");
		assertThat(markdown).contains("ExecutionExecutorIntegrationResult");
		assertThat(markdown).contains("ExecutionExecutorIntegrationStatus");
		assertThat(markdown).contains("ExecutionExecutorIntegrationReason");
		assertThat(markdown).contains("ExecutionExecutorIntegrationScope");

		assertThat(markdown).contains("ExecutionExecutor는 Execution Adapter를 통해 Runtime Execution Layer에 위임 가능한 상태를 표현하는 Semantic Layer이다.");
		assertThat(markdown).contains("ExecutionExecutor는 read-only이다.");
		assertThat(markdown).contains("ExecutionExecutor는 실제 Executor 구현이 아니다.");
		assertThat(markdown).contains("ExecutionExecutor는 Executor Thread 생성이 아니다.");
		assertThat(markdown).contains("ExecutionExecutor는 Adapter 호출이 아니다.");
		assertThat(markdown).contains("ExecutionExecutor는 Kubernetes API 호출이 아니다.");
		assertThat(markdown).contains("ExecutionExecutor는 kubectl 실행이 아니다.");
		assertThat(markdown).contains("ExecutionExecutor는 ArgoCD Sync가 아니다.");
		assertThat(markdown).contains("ExecutionExecutor는 Terraform/OpenTofu Apply가 아니다.");
		assertThat(markdown).contains("ExecutionExecutor는 SSH/Ansible 실행이 아니다.");
		assertThat(markdown).contains("ExecutionExecutor는 실제 Action 실행이 아니다.");
		assertThat(markdown).contains("ExecutionExecutor는 ExecutionAdapterIntegration에 의존한다.");
		assertThat(markdown).contains("EXECUTION_EXECUTOR_READY만 execution executor 후보가 될 수 있다.");
		assertThat(markdown).contains("executorIdentifier는 필수이다.");
		assertThat(markdown).contains("executionStrategy는 필수이다.");
		assertThat(markdown).contains("executionBoundary는 필수이다.");
		assertThat(markdown).contains("executorPolicy는 필수이다.");
		assertThat(markdown).contains("payment safety uncertainty는 BLOCKED이다.");
		assertThat(markdown).contains("critical lifecycle risk는 BLOCKED이다.");
		assertThat(markdown).contains("ExecutionExecutorIntegration은 execution executor readiness 해석 계층이다.");
		assertThat(markdown).contains("EXECUTION_EXECUTOR_READY_VIEW는 실제 Runtime Execution이 아니다.");
		assertThat(markdown).contains("ExecutionExecutorIntegration은 execution authority가 아니다.");
		assertThat(markdown).contains("ExecutionExecutorIntegration은 action authority가 아니다.");
		assertThat(markdown).contains("ExecutionExecutorIntegration은 runtime executor implementation이 아니다.");
		assertThat(markdown).contains("portfolio knowledge source 수정 금지.");

		assertThat(markdown).contains("Execution Executor");
		assertThat(markdown).contains("Runtime Execution");
		assertThat(markdown).contains("Executor Implementation");
		assertThat(markdown).contains("Executor Thread");
		assertThat(markdown).contains("Adapter Invocation");
		assertThat(markdown).contains("Kubernetes API");
		assertThat(markdown).contains("kubectl");
		assertThat(markdown).contains("ArgoCD Sync");
		assertThat(markdown).contains("Terraform/OpenTofu Apply");
		assertThat(markdown).contains("SSH / Ansible");
		assertThat(markdown).contains("Action Execution");

		assertThat(markdown).contains("Runtime Execution Implementation");
		assertThat(markdown).contains("Executor Thread Management");
		assertThat(markdown).contains("Adapter Invocation");
		assertThat(markdown).contains("Kubernetes API Integration");
		assertThat(markdown).contains("kubectl Integration");
		assertThat(markdown).contains("ArgoCD Sync Integration");
		assertThat(markdown).contains("Terraform/OpenTofu Apply Integration");
		assertThat(markdown).contains("SSH / Ansible Execution");
		assertThat(markdown).contains("Execution Audit History");
		assertThat(markdown).contains("Runtime Execution Monitoring");
		assertThat(markdown).contains("Runtime Rollback Workflow");

		assertThat(readmeMarkdown).contains(
				"### Runtime Operational Execution Executor Phase Closure"
		);
		assertThat(readmeMarkdown).contains(
				"docs/runtime-operational-execution-executor-phase-closure.md"
		);
	}
}
