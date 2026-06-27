package com.fintech.sre.agent.runtime.execution;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class OperationalReliabilityExecutionPlanPhaseClosureResourceTest {

	@Test
	void shouldContainOperationalExecutionPlanPhaseClosure()
			throws IOException {
		Path document = Path.of(
				"docs",
				"runtime-operational-execution-plan-phase-closure.md"
		);
		Path readme = Path.of("README.md");

		assertThat(Files.exists(document)).isTrue();
		assertThat(Files.exists(readme)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);
		String readmeMarkdown = Files.readString(readme, StandardCharsets.UTF_8);

		assertThat(markdown).contains(
				"# Runtime Operational Execution Plan Phase Closure"
		);
		assertThat(markdown).contains("## 1. Purpose");
		assertThat(markdown).contains("## 2. Completed Scope");
		assertThat(markdown).contains("## 3. Execution Plan Semantics");
		assertThat(markdown).contains("## 4. Execution Permission Dependency");
		assertThat(markdown).contains("## 5. Required Execution Plan Conditions");
		assertThat(markdown).contains("## 6. Execution Plan Integration Semantics");
		assertThat(markdown).contains("## 7. Execution Plan Readiness Boundary");
		assertThat(markdown).contains("## 8. Payment Safety Boundary");
		assertThat(markdown).contains("## 9. Lifecycle Uncertainty Boundary");
		assertThat(markdown).contains("## 10. Runtime Invariants");
		assertThat(markdown).contains("## 11. Deferred Scope");
		assertThat(markdown).contains("## 12. Non-Goals");
		assertThat(markdown).contains("## 13. Phase Closure Summary");

		assertThat(markdown).contains("ExecutionPlan");
		assertThat(markdown).contains("ExecutionPlanEvaluator");
		assertThat(markdown).contains("ExecutionPlanLevel");
		assertThat(markdown).contains("ExecutionPlanReason");
		assertThat(markdown).contains("ExecutionPlanScope");
		assertThat(markdown).contains("ExecutionPlanIntegration");
		assertThat(markdown).contains("ExecutionPlanIntegrationResult");
		assertThat(markdown).contains("ExecutionPlanIntegrationStatus");
		assertThat(markdown).contains("ExecutionPlanIntegrationReason");
		assertThat(markdown).contains("ExecutionPlanIntegrationScope");

		assertThat(markdown).contains("ExecutionPlan은 실행 계획을 표현하는 Runtime Semantic Layer이다.");
		assertThat(markdown).contains("ExecutionPlan은 read-only이다.");
		assertThat(markdown).contains("ExecutionPlan은 actual action execution이 아니다.");
		assertThat(markdown).contains("ExecutionPlan은 action dispatch가 아니다.");
		assertThat(markdown).contains("ExecutionPlan은 Kubernetes API 호출이 아니다.");
		assertThat(markdown).contains("ExecutionPlan은 kubectl 실행이 아니다.");
		assertThat(markdown).contains("ExecutionPlan은 ArgoCD Sync가 아니다.");
		assertThat(markdown).contains("ExecutionPlan은 Terraform/OpenTofu Apply가 아니다.");
		assertThat(markdown).contains("ExecutionPlan은 SSH/Ansible 실행이 아니다.");
		assertThat(markdown).contains("ExecutionPlan은 Execution Engine이 아니다.");
		assertThat(markdown).contains("ExecutionPlan은 ExecutionPermissionIntegration에 의존한다.");
		assertThat(markdown).contains("EXECUTION_PLAN_READY만 execution plan 후보가 될 수 있다.");
		assertThat(markdown).contains("executionPlanIdentifier는 필수이다.");
		assertThat(markdown).contains("executionSequence는 필수이다.");
		assertThat(markdown).contains("rollbackPlan은 필수이다.");
		assertThat(markdown).contains("verificationPlan은 필수이다.");
		assertThat(markdown).contains("payment safety uncertainty는 BLOCKED이다.");
		assertThat(markdown).contains("critical lifecycle risk는 BLOCKED이다.");
		assertThat(markdown).contains("ExecutionPlanIntegration은 execution plan readiness 해석 계층이다.");
		assertThat(markdown).contains("EXECUTION_PLAN_READY_VIEW는 실제 실행 계획 수행이 아니다.");
		assertThat(markdown).contains("ExecutionPlanIntegration은 action authority가 아니다.");
		assertThat(markdown).contains("ExecutionPlanIntegration은 dispatch authority가 아니다.");
		assertThat(markdown).contains("ExecutionPlanIntegration은 execution engine이 아니다.");
		assertThat(markdown).contains("portfolio knowledge source 수정 금지.");

		assertThat(markdown).contains("Execution Plan");
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
				"### Runtime Operational Execution Plan Phase Closure"
		);
		assertThat(readmeMarkdown).contains(
				"docs/runtime-operational-execution-plan-phase-closure.md"
		);
	}
}
