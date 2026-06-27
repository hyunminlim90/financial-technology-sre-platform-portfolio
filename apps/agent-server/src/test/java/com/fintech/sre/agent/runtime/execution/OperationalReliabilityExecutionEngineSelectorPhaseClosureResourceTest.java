package com.fintech.sre.agent.runtime.execution;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class OperationalReliabilityExecutionEngineSelectorPhaseClosureResourceTest {

	@Test
	void shouldContainOperationalExecutionEngineSelectorPhaseClosure()
			throws IOException {
		Path document = Path.of(
				"docs",
				"runtime-operational-execution-engine-selector-phase-closure.md"
		);
		Path readme = Path.of("README.md");

		assertThat(Files.exists(document)).isTrue();
		assertThat(Files.exists(readme)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);
		String readmeMarkdown = Files.readString(readme, StandardCharsets.UTF_8);

		assertThat(markdown).contains(
				"# Runtime Operational Execution Engine Selector Phase Closure"
		);
		assertThat(markdown).contains("## 1. Purpose");
		assertThat(markdown).contains("## 2. Completed Scope");
		assertThat(markdown).contains("## 3. Execution Engine Selector Semantics");
		assertThat(markdown).contains("## 4. Execution Engine Registry Dependency");
		assertThat(markdown).contains(
				"## 5. Required Execution Engine Selector Conditions"
		);
		assertThat(markdown).contains(
				"## 6. Execution Engine Selector Integration Semantics"
		);
		assertThat(markdown).contains("## 7. Selector Readiness Boundary");
		assertThat(markdown).contains("## 8. Payment Safety Boundary");
		assertThat(markdown).contains("## 9. Lifecycle Uncertainty Boundary");
		assertThat(markdown).contains("## 10. Runtime Invariants");
		assertThat(markdown).contains("## 11. Deferred Scope");
		assertThat(markdown).contains("## 12. Non-Goals");
		assertThat(markdown).contains("## 13. Phase Closure Summary");

		assertThat(markdown).contains("ExecutionEngineSelector");
		assertThat(markdown).contains("ExecutionEngineSelectorEvaluator");
		assertThat(markdown).contains("ExecutionEngineSelectorLevel");
		assertThat(markdown).contains("ExecutionEngineSelectorReason");
		assertThat(markdown).contains("ExecutionEngineSelectorScope");
		assertThat(markdown).contains("ExecutionEngineSelectorIntegration");
		assertThat(markdown).contains("ExecutionEngineSelectorIntegrationResult");
		assertThat(markdown).contains("ExecutionEngineSelectorIntegrationStatus");
		assertThat(markdown).contains("ExecutionEngineSelectorIntegrationReason");
		assertThat(markdown).contains("ExecutionEngineSelectorIntegrationScope");

		assertThat(markdown).contains("ExecutionEngineSelector는 Execution Engine Registry 기반 실행 엔진 선택 가능 상태를 표현하는 Semantic Layer이다.");
		assertThat(markdown).contains("ExecutionEngineSelector는 read-only이다.");
		assertThat(markdown).contains("ExecutionEngineSelector는 실제 Engine Selection이 아니다.");
		assertThat(markdown).contains("ExecutionEngineSelector는 Registry 조회가 아니다.");
		assertThat(markdown).contains("ExecutionEngineSelector는 Engine Discovery가 아니다.");
		assertThat(markdown).contains("ExecutionEngineSelector는 Spring Bean 조회가 아니다.");
		assertThat(markdown).contains("ExecutionEngineSelector는 ServiceLoader 조회가 아니다.");
		assertThat(markdown).contains("ExecutionEngineSelector는 실제 Action 실행이 아니다.");
		assertThat(markdown).contains("ExecutionEngineSelector는 ExecutionEngineRegistryIntegration에 의존한다.");
		assertThat(markdown).contains("EXECUTION_ENGINE_SELECTOR_READY만 execution engine selector 후보가 될 수 있다.");
		assertThat(markdown).contains("selectorIdentifier는 필수이다.");
		assertThat(markdown).contains("engineSelectionPolicy는 필수이다.");
		assertThat(markdown).contains("engineCapabilityRequirement는 필수이다.");
		assertThat(markdown).contains("selectorGuardrail은 필수이다.");
		assertThat(markdown).contains("payment safety uncertainty는 BLOCKED이다.");
		assertThat(markdown).contains("critical lifecycle risk는 BLOCKED이다.");
		assertThat(markdown).contains("ExecutionEngineSelectorIntegration은 execution engine selector readiness 해석 계층이다.");
		assertThat(markdown).contains("EXECUTION_ENGINE_SELECTOR_READY_VIEW는 실제 engine selection이 아니다.");
		assertThat(markdown).contains("ExecutionEngineSelectorIntegration은 registry lookup authority가 아니다.");
		assertThat(markdown).contains("ExecutionEngineSelectorIntegration은 engine discovery authority가 아니다.");
		assertThat(markdown).contains("ExecutionEngineSelectorIntegration은 execution authority가 아니다.");
		assertThat(markdown).contains("portfolio knowledge source 수정 금지.");

		assertThat(markdown).contains("Execution Engine Selector");
		assertThat(markdown).contains("Actual Engine Selection");
		assertThat(markdown).contains("Registry Lookup");
		assertThat(markdown).contains("Engine Discovery");
		assertThat(markdown).contains("Spring Bean Lookup");
		assertThat(markdown).contains("ServiceLoader Lookup");
		assertThat(markdown).contains("Action Execution");
		assertThat(markdown).contains("Execution Authority");

		assertThat(markdown).contains("Actual Engine Selection");
		assertThat(markdown).contains("Registry Lookup");
		assertThat(markdown).contains("Engine Discovery");
		assertThat(markdown).contains("Spring Bean Lookup");
		assertThat(markdown).contains("ServiceLoader Lookup");
		assertThat(markdown).contains("Execution Engine Resolver");
		assertThat(markdown).contains("Execution Adapter Selection");
		assertThat(markdown).contains("Action Execution");
		assertThat(markdown).contains("Execution Audit History");
		assertThat(markdown).contains("Execution Engine Capability Matching");

		assertThat(readmeMarkdown).contains(
				"### Runtime Operational Execution Engine Selector Phase Closure"
		);
		assertThat(readmeMarkdown).contains(
				"docs/runtime-operational-execution-engine-selector-phase-closure.md"
		);
	}
}
