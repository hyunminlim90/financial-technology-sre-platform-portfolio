package com.fintech.sre.agent.runtime.execution;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class OperationalReliabilityExecutionEngineRegistryPhaseClosureResourceTest {

	@Test
	void shouldContainOperationalExecutionEngineRegistryPhaseClosure()
			throws IOException {
		Path document = Path.of(
				"docs",
				"runtime-operational-execution-engine-registry-phase-closure.md"
		);
		Path readme = Path.of("README.md");

		assertThat(Files.exists(document)).isTrue();
		assertThat(Files.exists(readme)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);
		String readmeMarkdown = Files.readString(readme, StandardCharsets.UTF_8);

		assertThat(markdown).contains(
				"# Runtime Operational Execution Engine Registry Phase Closure"
		);
		assertThat(markdown).contains("## 1. Purpose");
		assertThat(markdown).contains("## 2. Completed Scope");
		assertThat(markdown).contains("## 3. Execution Engine Registry Semantics");
		assertThat(markdown).contains("## 4. Execution Engine Dependency");
		assertThat(markdown).contains(
				"## 5. Required Execution Engine Registry Conditions"
		);
		assertThat(markdown).contains(
				"## 6. Execution Engine Registry Integration Semantics"
		);
		assertThat(markdown).contains("## 7. Registry Readiness Boundary");
		assertThat(markdown).contains("## 8. Payment Safety Boundary");
		assertThat(markdown).contains("## 9. Lifecycle Uncertainty Boundary");
		assertThat(markdown).contains("## 10. Runtime Invariants");
		assertThat(markdown).contains("## 11. Deferred Scope");
		assertThat(markdown).contains("## 12. Non-Goals");
		assertThat(markdown).contains("## 13. Phase Closure Summary");

		assertThat(markdown).contains("ExecutionEngineRegistry");
		assertThat(markdown).contains("ExecutionEngineRegistryEvaluator");
		assertThat(markdown).contains("ExecutionEngineRegistryLevel");
		assertThat(markdown).contains("ExecutionEngineRegistryReason");
		assertThat(markdown).contains("ExecutionEngineRegistryScope");
		assertThat(markdown).contains("ExecutionEngineRegistryIntegration");
		assertThat(markdown).contains("ExecutionEngineRegistryIntegrationResult");
		assertThat(markdown).contains("ExecutionEngineRegistryIntegrationStatus");
		assertThat(markdown).contains("ExecutionEngineRegistryIntegrationReason");
		assertThat(markdown).contains("ExecutionEngineRegistryIntegrationScope");

		assertThat(markdown).contains("ExecutionEngineRegistry는 Runtime에서 사용 가능한 Execution Engine Registry 상태를 표현하는 Semantic Layer이다.");
		assertThat(markdown).contains("ExecutionEngineRegistry는 read-only이다.");
		assertThat(markdown).contains("ExecutionEngineRegistry는 Registry 구현이 아니다.");
		assertThat(markdown).contains("ExecutionEngineRegistry는 Engine Discovery가 아니다.");
		assertThat(markdown).contains("ExecutionEngineRegistry는 Spring Bean Registry가 아니다.");
		assertThat(markdown).contains("ExecutionEngineRegistry는 ServiceLoader가 아니다.");
		assertThat(markdown).contains("ExecutionEngineRegistry는 실제 Execution Engine 선택이 아니다.");
		assertThat(markdown).contains("ExecutionEngineRegistry는 실제 Action 실행이 아니다.");
		assertThat(markdown).contains("ExecutionEngineRegistry는 ExecutionEngineIntegration에 의존한다.");
		assertThat(markdown).contains("EXECUTION_ENGINE_REGISTRY_READY만 execution engine registry 후보가 될 수 있다.");
		assertThat(markdown).contains("registryIdentifier는 필수이다.");
		assertThat(markdown).contains("engineRegistration은 필수이다.");
		assertThat(markdown).contains("registryPolicy는 필수이다.");
		assertThat(markdown).contains("registryGuardrail은 필수이다.");
		assertThat(markdown).contains("payment safety uncertainty는 BLOCKED이다.");
		assertThat(markdown).contains("critical lifecycle risk는 BLOCKED이다.");
		assertThat(markdown).contains("ExecutionEngineRegistryIntegration은 execution engine registry readiness 해석 계층이다.");
		assertThat(markdown).contains("EXECUTION_ENGINE_REGISTRY_READY_VIEW는 실제 registry 구현이 아니다.");
		assertThat(markdown).contains("ExecutionEngineRegistryIntegration은 engine discovery authority가 아니다.");
		assertThat(markdown).contains("ExecutionEngineRegistryIntegration은 engine selection authority가 아니다.");
		assertThat(markdown).contains("ExecutionEngineRegistryIntegration은 execution authority가 아니다.");
		assertThat(markdown).contains("portfolio knowledge source 수정 금지.");

		assertThat(markdown).contains("Execution Engine Registry");
		assertThat(markdown).contains("Registry Implementation");
		assertThat(markdown).contains("Engine Discovery");
		assertThat(markdown).contains("Spring Bean Registry");
		assertThat(markdown).contains("ServiceLoader");
		assertThat(markdown).contains("Execution Engine Selection");
		assertThat(markdown).contains("Action Execution");
		assertThat(markdown).contains("Execution Authority");

		assertThat(markdown).contains("Actual Registry Implementation");
		assertThat(markdown).contains("Engine Discovery");
		assertThat(markdown).contains("Spring Bean Registry Integration");
		assertThat(markdown).contains("ServiceLoader Integration");
		assertThat(markdown).contains("Execution Engine Selection");
		assertThat(markdown).contains("Execution Engine Adapter Registration");
		assertThat(markdown).contains("Action Execution");
		assertThat(markdown).contains("Execution Audit History");
		assertThat(markdown).contains("Execution Engine Health Check");
		assertThat(markdown).contains("Execution Engine Capability Matching");

		assertThat(readmeMarkdown).contains(
				"### Runtime Operational Execution Engine Registry Phase Closure"
		);
		assertThat(readmeMarkdown).contains(
				"docs/runtime-operational-execution-engine-registry-phase-closure.md"
		);
	}
}
