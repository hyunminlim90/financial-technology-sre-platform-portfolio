package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class OperationalReliabilityObservableRuntimePhaseClosureResourceTest {

	@Test
	void shouldContainOperationalReliabilityObservableRuntimePhaseClosure()
			throws IOException {
		Path document = Path.of(
				"docs",
				"runtime-operational-reliability-observable-runtime-phase-closure.md"
		);
		Path readme = Path.of("README.md");

		assertThat(Files.exists(document)).isTrue();
		assertThat(Files.exists(readme)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);
		String readmeMarkdown = Files.readString(readme, StandardCharsets.UTF_8);

		assertThat(markdown).contains(
				"# Runtime Operational Reliability Observable Runtime Phase Closure"
		);
		assertThat(markdown).contains("## 1. Purpose");
		assertThat(markdown).contains("## 2. Completed Scope");
		assertThat(markdown).contains("## 3. Observable Runtime Pipeline");
		assertThat(markdown).contains("## 4. Vendor-Neutral Evidence Boundary");
		assertThat(markdown).contains("## 5. Normalized Evidence Semantics");
		assertThat(markdown).contains("## 6. Payment Consistency Evidence Rule");
		assertThat(markdown).contains("## 7. Read-only Runtime Boundary");
		assertThat(markdown).contains("## 8. Runtime Invariants");
		assertThat(markdown).contains("## 9. Deferred Scope");
		assertThat(markdown).contains("## 10. Non-Goals");
		assertThat(markdown).contains("## 11. Phase Closure Summary");

		assertThat(markdown).contains("EvidenceAdapterPort");
		assertThat(markdown).contains("EvidenceQuery");
		assertThat(markdown).contains("EvidenceQueryResult");
		assertThat(markdown).contains("EvidenceSourceType");
		assertThat(markdown).contains("EvidenceCollectionStatus");
		assertThat(markdown).contains("EvidenceCollectionOrchestrator");
		assertThat(markdown).contains("EvidenceCollectionResult");
		assertThat(markdown).contains("EvidenceAssessmentPipeline");
		assertThat(markdown).contains("AssessmentLifecyclePipeline");
		assertThat(markdown).contains("ObservableReliabilityRuntimePipeline");
		assertThat(markdown).contains("ReliabilityLifecycleSummaryResource");

		assertThat(markdown).contains(
				"EvidenceCollectionOrchestrator\n→ EvidenceAssessmentPipeline\n→ AssessmentLifecyclePipeline\n→ ReliabilityLifecycleSummaryResource"
		);

		assertThat(markdown).contains("raw observability payload is never exposed directly to semantic runtime");
		assertThat(markdown).contains("Prometheus/Loki/Tempo/vendor detail does not leak into the assessment layer");
		assertThat(markdown).contains("EvidenceQueryResult returns normalized semantic evidence only");
		assertThat(markdown).contains("UNKNOWN/PARTIAL evidence remains uncertainty, not false certainty");
		assertThat(markdown).contains("payment consistency evidence missing keeps payment safety uncertainty active");
		assertThat(markdown).contains("payment inconsistency propagates to CRITICAL lifecycle summary risk");
		assertThat(markdown).contains("observable runtime pipeline is read-only");
		assertThat(markdown).contains("observable runtime pipeline is recommendation-neutral");
		assertThat(markdown).contains("observable runtime pipeline is execution-permission-neutral");
		assertThat(markdown).contains("observable runtime pipeline does not call an executor");
		assertThat(markdown).contains("portfolio knowledge source is never modified");

		assertThat(markdown).contains("Prometheus adapter implementation");
		assertThat(markdown).contains("Loki adapter implementation");
		assertThat(markdown).contains("Tempo adapter implementation");
		assertThat(markdown).contains("CloudWatch adapter implementation");
		assertThat(markdown).contains("WebClient / Reactor integration");
		assertThat(markdown).contains("scheduler / event stream integration");
		assertThat(markdown).contains("persistent evidence store");
		assertThat(markdown).contains("WebFlux API exposure");
		assertThat(markdown).contains("SRE Console integration");

		assertThat(readmeMarkdown).contains(
				"### Runtime Operational Reliability Observable Runtime Phase Closure"
		);
		assertThat(readmeMarkdown).contains(
				"docs/runtime-operational-reliability-observable-runtime-phase-closure.md"
		);
	}
}
