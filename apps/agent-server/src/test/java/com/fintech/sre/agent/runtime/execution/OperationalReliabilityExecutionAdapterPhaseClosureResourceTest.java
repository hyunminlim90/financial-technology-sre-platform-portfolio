package com.fintech.sre.agent.runtime.execution;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class OperationalReliabilityExecutionAdapterPhaseClosureResourceTest {

	@Test
	void shouldContainOperationalExecutionAdapterPhaseClosure()
			throws IOException {
		Path document = Path.of(
				"docs",
				"runtime-operational-execution-adapter-phase-closure.md"
		);
		Path readme = Path.of("README.md");

		assertThat(Files.exists(document)).isTrue();
		assertThat(Files.exists(readme)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);
		String readmeMarkdown = Files.readString(readme, StandardCharsets.UTF_8);

		assertThat(markdown).contains(
				"# Runtime Operational Execution Adapter Phase Closure"
		);
		assertThat(markdown).contains("## 1. Purpose");
		assertThat(markdown).contains("## 2. Completed Scope");
		assertThat(markdown).contains("## 3. Execution Adapter Semantics");
		assertThat(markdown).contains("## 4. Execution Engine Selector Dependency");
		assertThat(markdown).contains(
				"## 5. Required Execution Adapter Conditions"
		);
		assertThat(markdown).contains(
				"## 6. Execution Adapter Integration Semantics"
		);
		assertThat(markdown).contains("## 7. Adapter Readiness Boundary");
		assertThat(markdown).contains("## 8. Payment Safety Boundary");
		assertThat(markdown).contains("## 9. Lifecycle Uncertainty Boundary");
		assertThat(markdown).contains("## 10. Runtime Invariants");
		assertThat(markdown).contains("## 11. Deferred Scope");
		assertThat(markdown).contains("## 12. Non-Goals");
		assertThat(markdown).contains("## 13. Phase Closure Summary");

		assertThat(markdown).contains("ExecutionAdapter");
		assertThat(markdown).contains("ExecutionAdapterEvaluator");
		assertThat(markdown).contains("ExecutionAdapterLevel");
		assertThat(markdown).contains("ExecutionAdapterReason");
		assertThat(markdown).contains("ExecutionAdapterScope");
		assertThat(markdown).contains("ExecutionAdapterIntegration");
		assertThat(markdown).contains("ExecutionAdapterIntegrationResult");
		assertThat(markdown).contains("ExecutionAdapterIntegrationStatus");
		assertThat(markdown).contains("ExecutionAdapterIntegrationReason");
		assertThat(markdown).contains("ExecutionAdapterIntegrationScope");

		assertThat(markdown).contains("ExecutionAdapter는 선택된 Execution Engine을 실행 계층에 연결 가능한 상태를 표현하는 Semantic Layer이다.");
		assertThat(markdown).contains("ExecutionAdapter는 read-only이다.");
		assertThat(markdown).contains("ExecutionAdapter는 실제 Adapter 구현이 아니다.");
		assertThat(markdown).contains("ExecutionAdapter는 Adapter 호출이 아니다.");
		assertThat(markdown).contains("ExecutionAdapter는 Kubernetes / ArgoCD / Terraform / SSH / Ansible Adapter가 아니다.");
		assertThat(markdown).contains("ExecutionAdapter는 실제 Action 실행이 아니다.");
		assertThat(markdown).contains("ExecutionAdapter는 ExecutionEngineSelectorIntegration에 의존한다.");
		assertThat(markdown).contains("EXECUTION_ADAPTER_READY만 adapter 후보가 될 수 있다.");
		assertThat(markdown).contains("adapterIdentifier는 필수이다.");
		assertThat(markdown).contains("adapterType는 필수이다.");
		assertThat(markdown).contains("adapterBinding는 필수이다.");
		assertThat(markdown).contains("adapterPolicy는 필수이다.");
		assertThat(markdown).contains("payment safety uncertainty는 BLOCKED이다.");
		assertThat(markdown).contains("critical lifecycle risk는 BLOCKED이다.");
		assertThat(markdown).contains("ExecutionAdapterIntegration은 operator-facing / lifecycle semantics 해석 계층이다.");
		assertThat(markdown).contains("EXECUTION_ADAPTER_READY_VIEW는 실제 Adapter 호출이 아니다.");
		assertThat(markdown).contains("portfolio knowledge source 수정 금지.");

		assertThat(markdown).contains("Execution Adapter");
		assertThat(markdown).contains("Adapter Implementation");
		assertThat(markdown).contains("Adapter Invocation");
		assertThat(markdown).contains("Kubernetes Adapter");
		assertThat(markdown).contains("ArgoCD Adapter");
		assertThat(markdown).contains("Terraform/OpenTofu Adapter");
		assertThat(markdown).contains("SSH / Ansible Adapter");
		assertThat(markdown).contains("Action Execution");

		assertThat(markdown).contains("Actual Adapter Implementation");
		assertThat(markdown).contains("Actual Adapter Invocation");
		assertThat(markdown).contains("Kubernetes Adapter Implementation");
		assertThat(markdown).contains("ArgoCD Adapter Implementation");
		assertThat(markdown).contains("Terraform/OpenTofu Adapter Implementation");
		assertThat(markdown).contains("SSH / Ansible Adapter Implementation");
		assertThat(markdown).contains("Adapter Audit History");
		assertThat(markdown).contains("Adapter Health Check");
		assertThat(markdown).contains("Adapter Capability Matching");

		assertThat(readmeMarkdown).contains(
				"### Runtime Operational Execution Adapter Phase Closure"
		);
		assertThat(readmeMarkdown).contains(
				"docs/runtime-operational-execution-adapter-phase-closure.md"
		);
	}
}
