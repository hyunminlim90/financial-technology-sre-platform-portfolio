package com.fintech.sre.agent.runtime.execution;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class OperationalReliabilityExecutionSessionPhaseClosureResourceTest {

	@Test
	void shouldContainOperationalExecutionSessionPhaseClosure()
			throws IOException {
		Path document = Path.of(
				"docs",
				"runtime-operational-execution-session-phase-closure.md"
		);
		Path readme = Path.of("README.md");

		assertThat(Files.exists(document)).isTrue();
		assertThat(Files.exists(readme)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);
		String readmeMarkdown = Files.readString(readme, StandardCharsets.UTF_8);

		assertThat(markdown).contains(
				"# Runtime Operational Execution Session Phase Closure"
		);
		assertThat(markdown).contains("## 1. Purpose");
		assertThat(markdown).contains("## 2. Completed Scope");
		assertThat(markdown).contains("## 3. Execution Session Semantics");
		assertThat(markdown).contains("## 4. Execution Executor Dependency");
		assertThat(markdown).contains(
				"## 5. Required Execution Session Conditions"
		);
		assertThat(markdown).contains(
				"## 6. Execution Session Integration Semantics"
		);
		assertThat(markdown).contains("## 7. Session Readiness Boundary");
		assertThat(markdown).contains("## 8. Payment Safety Boundary");
		assertThat(markdown).contains("## 9. Lifecycle Uncertainty Boundary");
		assertThat(markdown).contains("## 10. Runtime Invariants");
		assertThat(markdown).contains("## 11. Deferred Scope");
		assertThat(markdown).contains("## 12. Non-Goals");
		assertThat(markdown).contains("## 13. Phase Closure Summary");

		assertThat(markdown).contains("ExecutionSession");
		assertThat(markdown).contains("ExecutionSessionEvaluator");
		assertThat(markdown).contains("ExecutionSessionLevel");
		assertThat(markdown).contains("ExecutionSessionReason");
		assertThat(markdown).contains("ExecutionSessionScope");
		assertThat(markdown).contains("ExecutionSessionIntegration");
		assertThat(markdown).contains("ExecutionSessionIntegrationResult");
		assertThat(markdown).contains("ExecutionSessionIntegrationStatus");
		assertThat(markdown).contains("ExecutionSessionIntegrationReason");
		assertThat(markdown).contains("ExecutionSessionIntegrationScope");

		assertThat(markdown).contains("ExecutionSession는 Runtime Execution을 식별하는 논리적 Session 준비 상태를 표현하는 Semantic Layer이다.");
		assertThat(markdown).contains("ExecutionSession는 read-only이다.");
		assertThat(markdown).contains("ExecutionSession는 실제 Session 생성이 아니다.");
		assertThat(markdown).contains("ExecutionSession는 Thread 생성이 아니다.");
		assertThat(markdown).contains("ExecutionSession는 Transaction 시작이 아니다.");
		assertThat(markdown).contains("ExecutionSession는 Kubernetes Job 생성이 아니다.");
		assertThat(markdown).contains("ExecutionSession는 Pod 생성이 아니다.");
		assertThat(markdown).contains("ExecutionSession는 Workflow 실행이 아니다.");
		assertThat(markdown).contains("ExecutionSession는 Runtime Execution 수행이 아니다.");
		assertThat(markdown).contains("ExecutionSession는 실제 Action 실행이 아니다.");
		assertThat(markdown).contains("ExecutionSession는 ExecutionExecutorIntegration에 의존한다.");
		assertThat(markdown).contains("EXECUTION_SESSION_READY만 execution session 후보가 될 수 있다.");
		assertThat(markdown).contains("sessionIdentifier는 필수이다.");
		assertThat(markdown).contains("executionCorrelationIdentifier는 필수이다.");
		assertThat(markdown).contains("executionScope는 필수이다.");
		assertThat(markdown).contains("sessionPolicy는 필수이다.");
		assertThat(markdown).contains("payment safety uncertainty는 BLOCKED이다.");
		assertThat(markdown).contains("critical lifecycle risk는 BLOCKED이다.");
		assertThat(markdown).contains("ExecutionSessionIntegration은 execution session readiness 해석 계층이다.");
		assertThat(markdown).contains("EXECUTION_SESSION_READY_VIEW는 실제 Session 생성이 아니다.");
		assertThat(markdown).contains("ExecutionSessionIntegration은 execution authority가 아니다.");
		assertThat(markdown).contains("ExecutionSessionIntegration은 session creation authority가 아니다.");
		assertThat(markdown).contains("ExecutionSessionIntegration은 runtime execution implementation이 아니다.");
		assertThat(markdown).contains("portfolio knowledge source 수정 금지.");

		assertThat(markdown).contains("Execution Session");
		assertThat(markdown).contains("Actual Session Creation");
		assertThat(markdown).contains("Thread Creation");
		assertThat(markdown).contains("Transaction Start");
		assertThat(markdown).contains("Kubernetes Job");
		assertThat(markdown).contains("Pod Creation");
		assertThat(markdown).contains("Workflow Execution");
		assertThat(markdown).contains("Runtime Execution");
		assertThat(markdown).contains("Action Execution");

		assertThat(markdown).contains("Actual Session Creation");
		assertThat(markdown).contains("Thread Creation");
		assertThat(markdown).contains("Transaction Start");
		assertThat(markdown).contains("Kubernetes Job Creation");
		assertThat(markdown).contains("Pod Creation");
		assertThat(markdown).contains("Workflow Execution");
		assertThat(markdown).contains("Runtime Execution Implementation");
		assertThat(markdown).contains("Execution Context");
		assertThat(markdown).contains("Execution Result");
		assertThat(markdown).contains("Execution Audit History");
		assertThat(markdown).contains("Runtime Execution Monitoring");
		assertThat(markdown).contains("Runtime Rollback Workflow");

		assertThat(readmeMarkdown).contains(
				"### Runtime Operational Execution Session Phase Closure"
		);
		assertThat(readmeMarkdown).contains(
				"docs/runtime-operational-execution-session-phase-closure.md"
		);
	}
}
