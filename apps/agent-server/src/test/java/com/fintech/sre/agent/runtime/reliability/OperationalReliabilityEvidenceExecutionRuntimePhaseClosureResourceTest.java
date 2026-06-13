package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class OperationalReliabilityEvidenceExecutionRuntimePhaseClosureResourceTest {

	@Test
	void shouldContainOperationalReliabilityEvidenceExecutionRuntimePhaseClosure()
			throws IOException {
		Path document = Path.of(
				"docs",
				"runtime-operational-reliability-evidence-execution-runtime-phase-closure.md"
		);
		Path readme = Path.of("README.md");

		assertThat(Files.exists(document)).isTrue();
		assertThat(Files.exists(readme)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);
		String readmeMarkdown = Files.readString(readme, StandardCharsets.UTF_8);

		assertThat(markdown).contains(
				"# Runtime Operational Reliability Evidence Execution Runtime Phase Closure"
		);
		assertThat(markdown).contains("## 1. Purpose");
		assertThat(markdown).contains("## 2. Completed Scope");
		assertThat(markdown).contains("## 3. Executor Port Semantics");
		assertThat(markdown).contains("## 4. Dispatch Execution Semantics");
		assertThat(markdown).contains("## 5. Observable Runtime Integration");
		assertThat(markdown).contains("## 6. Payment Evidence Integrity Rule");
		assertThat(markdown).contains("## 7. Runtime Invariants");
		assertThat(markdown).contains("## 8. Deferred Scope");
		assertThat(markdown).contains("## 9. Non-Goals");
		assertThat(markdown).contains("## 10. Phase Closure Summary");

		assertThat(markdown).contains("EvidenceDispatchExecutorPort");
		assertThat(markdown).contains("EvidenceDispatchExecutionRequest");
		assertThat(markdown).contains("EvidenceDispatchExecutionResponse");
		assertThat(markdown).contains("EvidenceDispatchExecutionStatus");
		assertThat(markdown).contains("EvidenceDispatchExecutionRejectionReason");
		assertThat(markdown).contains("EvidenceDispatchExecutionPipeline");
		assertThat(markdown).contains("EvidenceDispatchExecutionPipelineInput");
		assertThat(markdown).contains("EvidenceDispatchExecutionPipelineResult");
		assertThat(markdown).contains("EvidenceDispatchExecutionPipelineStage");
		assertThat(markdown).contains("EvidenceDispatchExecutionPipelineRejectionReason");
		assertThat(markdown).contains("EvidenceExecutionObservablePipeline");
		assertThat(markdown).contains("EvidenceExecutionObservablePipelineInput");
		assertThat(markdown).contains("EvidenceExecutionObservablePipelineResult");
		assertThat(markdown).contains("EvidenceExecutionObservablePipelineStage");
		assertThat(markdown).contains("EvidenceExecutionObservablePipelineRejectionReason");

		assertThat(markdown).contains("executor port는 actual adapter implementation이 아님");
		assertThat(markdown).contains("executor port는 interface/contract only");
		assertThat(markdown).contains("dispatch execution은 normalized EvidenceQueryResult만 반환");
		assertThat(markdown).contains("raw payload 노출 금지");
		assertThat(markdown).contains("vendor detail 노출 금지");
		assertThat(markdown).contains("adapter execution failure != system failure");
		assertThat(markdown).contains("FAILED/UNKNOWN execution result는 evidence uncertainty로 전파");
		assertThat(markdown).contains("payment evidence integrity 없으면 payment safety uncertainty 유지");
		assertThat(markdown).contains("execution result는 observable runtime으로만 유입");
		assertThat(markdown).contains("observable runtime summary는 recommendation이 아님");
		assertThat(markdown).contains("observable runtime summary는 execution permission이 아님");
		assertThat(markdown).contains("recommendation authority 없음");
		assertThat(markdown).contains("execution authority 없음");
		assertThat(markdown).contains("portfolio knowledge source 수정 금지");

		assertThat(markdown).contains("actual Prometheus adapter invocation");
		assertThat(markdown).contains("actual Loki adapter invocation");
		assertThat(markdown).contains("actual Tempo adapter invocation");
		assertThat(markdown).contains("WebClient integration");
		assertThat(markdown).contains("Reactor integration");
		assertThat(markdown).contains("timeout policy");
		assertThat(markdown).contains("retry policy");
		assertThat(markdown).contains("adapter health check");
		assertThat(markdown).contains("persistent evidence store");
		assertThat(markdown).contains("WebFlux API exposure");
		assertThat(markdown).contains("scheduler/event stream integration");

		assertThat(readmeMarkdown).contains(
				"### Runtime Operational Reliability Evidence Execution Runtime Phase Closure"
		);
		assertThat(readmeMarkdown).contains(
				"docs/runtime-operational-reliability-evidence-execution-runtime-phase-closure.md"
		);
	}
}
