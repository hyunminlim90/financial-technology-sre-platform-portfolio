package com.fintech.sre.agent.runtime.execution;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class OperationalReliabilityExecutionContextPhaseClosureResourceTest {

	@Test
	void shouldContainOperationalExecutionContextPhaseClosure()
			throws IOException {
		Path document = Path.of(
				"docs",
				"runtime-operational-execution-context-phase-closure.md"
		);
		Path readme = Path.of("README.md");

		assertThat(Files.exists(document)).isTrue();
		assertThat(Files.exists(readme)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);
		String readmeMarkdown = Files.readString(readme, StandardCharsets.UTF_8);

		assertThat(markdown).contains(
				"# Runtime Operational Execution Context Phase Closure"
		);
		assertThat(markdown).contains("## 1. Purpose");
		assertThat(markdown).contains("## 2. Completed Scope");
		assertThat(markdown).contains("## 3. Execution Context Semantics");
		assertThat(markdown).contains("## 4. Execution Session Dependency");
		assertThat(markdown).contains(
				"## 5. Required Execution Context Conditions"
		);
		assertThat(markdown).contains(
				"## 6. Execution Context Integration Semantics"
		);
		assertThat(markdown).contains("## 7. Context Readiness Boundary");
		assertThat(markdown).contains("## 8. Payment Safety Boundary");
		assertThat(markdown).contains("## 9. Lifecycle Uncertainty Boundary");
		assertThat(markdown).contains("## 10. Runtime Invariants");
		assertThat(markdown).contains("## 11. Deferred Scope");
		assertThat(markdown).contains("## 12. Non-Goals");
		assertThat(markdown).contains("## 13. Phase Closure Summary");

		assertThat(markdown).contains("ExecutionContext");
		assertThat(markdown).contains("ExecutionContextEvaluator");
		assertThat(markdown).contains("ExecutionContextLevel");
		assertThat(markdown).contains("ExecutionContextReason");
		assertThat(markdown).contains("ExecutionContextScope");
		assertThat(markdown).contains("ExecutionContextIntegration");
		assertThat(markdown).contains("ExecutionContextIntegrationResult");
		assertThat(markdown).contains("ExecutionContextIntegrationStatus");
		assertThat(markdown).contains("ExecutionContextIntegrationReason");
		assertThat(markdown).contains("ExecutionContextIntegrationScope");

		assertThat(markdown).contains("ExecutionContext는 Runtime Execution에 필요한 논리적 실행 컨텍스트 준비 상태를 표현하는 Semantic Layer이다.");
		assertThat(markdown).contains("ExecutionContext는 read-only이다.");
		assertThat(markdown).contains("ExecutionContext는 실제 Context 생성이 아니다.");
		assertThat(markdown).contains("ExecutionContext는 ThreadLocal 생성이 아니다.");
		assertThat(markdown).contains("ExecutionContext는 SecurityContext 생성이 아니다.");
		assertThat(markdown).contains("ExecutionContext는 Transaction Context 생성이 아니다.");
		assertThat(markdown).contains("ExecutionContext는 Kubernetes Context 생성이 아니다.");
		assertThat(markdown).contains("ExecutionContext는 Runtime Execution 수행이 아니다.");
		assertThat(markdown).contains("ExecutionContext는 실제 Action 실행이 아니다.");
		assertThat(markdown).contains("ExecutionContext는 ExecutionSessionIntegration에 의존한다.");
		assertThat(markdown).contains("EXECUTION_CONTEXT_READY만 execution context 후보가 될 수 있다.");
		assertThat(markdown).contains("contextIdentifier는 필수이다.");
		assertThat(markdown).contains("executionContextScope는 필수이다.");
		assertThat(markdown).contains("executionMetadata는 필수이다.");
		assertThat(markdown).contains("contextPolicy는 필수이다.");
		assertThat(markdown).contains("payment safety uncertainty는 BLOCKED이다.");
		assertThat(markdown).contains("critical lifecycle risk는 BLOCKED이다.");
		assertThat(markdown).contains("ExecutionContextIntegration은 execution context readiness 해석 계층이다.");
		assertThat(markdown).contains("EXECUTION_CONTEXT_READY_VIEW는 실제 Context 생성이 아니다.");
		assertThat(markdown).contains("ExecutionContextIntegration은 execution authority가 아니다.");
		assertThat(markdown).contains("ExecutionContextIntegration은 context creation authority가 아니다.");
		assertThat(markdown).contains("ExecutionContextIntegration은 runtime execution implementation이 아니다.");
		assertThat(markdown).contains("portfolio knowledge source 수정 금지.");

		assertThat(markdown).contains("Execution Context");
		assertThat(markdown).contains("Actual Context Creation");
		assertThat(markdown).contains("ThreadLocal");
		assertThat(markdown).contains("SecurityContext");
		assertThat(markdown).contains("Transaction Context");
		assertThat(markdown).contains("Kubernetes Context");
		assertThat(markdown).contains("Runtime Execution");
		assertThat(markdown).contains("Action Execution");

		assertThat(markdown).contains("Actual Context Creation");
		assertThat(markdown).contains("ThreadLocal Context");
		assertThat(markdown).contains("SecurityContext");
		assertThat(markdown).contains("Transaction Context");
		assertThat(markdown).contains("Kubernetes Context");
		assertThat(markdown).contains("Runtime Execution Implementation");
		assertThat(markdown).contains("Execution Result");
		assertThat(markdown).contains("Execution Audit History");
		assertThat(markdown).contains("Runtime Execution Monitoring");
		assertThat(markdown).contains("Runtime Rollback Workflow");

		assertThat(readmeMarkdown).contains(
				"### Runtime Operational Execution Context Phase Closure"
		);
		assertThat(readmeMarkdown).contains(
				"docs/runtime-operational-execution-context-phase-closure.md"
		);
	}
}
