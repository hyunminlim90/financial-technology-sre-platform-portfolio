package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class OperationalReliabilitySemanticRuntimePhaseClosureResourceTest {

	@Test
	void shouldContainOperationalReliabilitySemanticRuntimePhaseClosure()
			throws IOException {
		Path document = Path.of(
				"docs",
				"runtime-operational-reliability-semantic-runtime-phase-closure.md"
		);
		Path readme = Path.of("README.md");

		assertThat(Files.exists(document)).isTrue();
		assertThat(Files.exists(readme)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);
		String readmeMarkdown = Files.readString(readme, StandardCharsets.UTF_8);

		assertThat(markdown).contains(
				"# Runtime Operational Reliability Semantic Runtime Phase Closure"
		);
		assertThat(markdown).contains("## 1. Purpose");
		assertThat(markdown).contains("## 2. Completed Scope");
		assertThat(markdown).contains("## 3. Semantic Runtime Layers");
		assertThat(markdown).contains("## 4. Runtime Boundaries");
		assertThat(markdown).contains("## 5. Governance Invariants");
		assertThat(markdown).contains("## 6. Payment Safety Invariants");
		assertThat(markdown).contains("## 7. Post-Execution Semantics");
		assertThat(markdown).contains("## 8. Lifecycle / Audit / Summary Semantics");
		assertThat(markdown).contains("## 9. Operator-Facing Semantics");
		assertThat(markdown).contains("## 10. Deferred Scope");
		assertThat(markdown).contains("## 11. Non-Goals");
		assertThat(markdown).contains("## 12. Phase Closure Summary");

		assertThat(markdown).contains("Evidence Correlation");
		assertThat(markdown).contains("Verification Gate");
		assertThat(markdown).contains("Convergence");
		assertThat(markdown).contains("Regression");
		assertThat(markdown).contains("Assessment Orchestrator");
		assertThat(markdown).contains("Risk Classification");
		assertThat(markdown).contains("Human Approval Policy");
		assertThat(markdown).contains("Recommendation Boundary");
		assertThat(markdown).contains("ActionCommand Boundary");
		assertThat(markdown).contains("Scenario Binding");
		assertThat(markdown).contains("Rollback/Verification Binding");
		assertThat(markdown).contains("Safety Policy Gate");
		assertThat(markdown).contains("Action Admission");
		assertThat(markdown).contains("Execution Boundary");
		assertThat(markdown).contains("Executor Contract");
		assertThat(markdown).contains("Execution Audit");
		assertThat(markdown).contains("Execution Readiness");
		assertThat(markdown).contains("Executor Port");
		assertThat(markdown).contains("Post-Execution Verification");
		assertThat(markdown).contains("Post-Execution Convergence");
		assertThat(markdown).contains("Post-Execution Regression");
		assertThat(markdown).contains("Lifecycle Orchestrator");
		assertThat(markdown).contains("Lifecycle Audit");
		assertThat(markdown).contains("Lifecycle Summary");
		assertThat(markdown).contains("Lifecycle Read Model Resource");

		assertThat(markdown).contains("semantic runtime != Kubernetes executor");
		assertThat(markdown).contains("semantic runtime != rollback executor");
		assertThat(markdown).contains("semantic runtime != observability collector");
		assertThat(markdown).contains("semantic runtime != approval workflow");
		assertThat(markdown).contains("semantic runtime != LLM/RAG engine");
		assertThat(markdown).contains("semantic runtime != production automation");

		assertThat(markdown).contains("No Scenario → No Recommendation");
		assertThat(markdown).contains("No Scenario → No ActionCommand");
		assertThat(markdown).contains("Assessment != Recommendation");
		assertThat(markdown).contains("Recommendation != ActionCommand");
		assertThat(markdown).contains("ActionAdmission != Execution Permission");
		assertThat(markdown).contains("Executor SUCCESS != VERIFIED");
		assertThat(markdown).contains("VERIFIED != CONVERGED");
		assertThat(markdown).contains("CONVERGED != Immutable Truth");
		assertThat(markdown).contains("Audit integrity required for trust");
		assertThat(markdown).contains("AI-only approval/execution forbidden");

		assertThat(markdown).contains("payment safety uncertainty blocks convergence admission");
		assertThat(markdown).contains("payment inconsistency elevates lifecycle and summary risk to CRITICAL");
		assertThat(markdown).contains("execution success is executor acknowledgement only");
		assertThat(markdown).contains("post-execution regression requires re-verification and re-convergence");
		assertThat(markdown).contains("hidden lifecycle decision is forbidden");
		assertThat(markdown).contains("summary is recommendation-neutral");
		assertThat(markdown).contains("resource layer is read-only");
		assertThat(markdown).contains("internal raw evidence payload is not exposed");

		assertThat(markdown).contains("Kubernetes executor adapter");
		assertThat(markdown).contains("Prometheus/Loki/Tempo evidence adapter");
		assertThat(markdown).contains("GitOps/ArgoCD/Argo Rollouts adapter");
		assertThat(markdown).contains("real approval workflow");
		assertThat(markdown).contains("persistent audit store");
		assertThat(markdown).contains("event stream integration");
		assertThat(markdown).contains("WebFlux API exposure");
		assertThat(markdown).contains("SRE Console UI integration");
		assertThat(markdown).contains("LLM/RAG explanation layer");

		assertThat(readmeMarkdown).contains(
				"### Runtime Operational Reliability Semantic Runtime Phase Closure"
		);
		assertThat(readmeMarkdown).contains(
				"docs/runtime-operational-reliability-semantic-runtime-phase-closure.md"
		);
	}
}
