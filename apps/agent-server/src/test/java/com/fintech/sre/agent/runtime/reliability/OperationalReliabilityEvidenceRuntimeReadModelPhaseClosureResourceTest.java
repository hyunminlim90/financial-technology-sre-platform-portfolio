package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class OperationalReliabilityEvidenceRuntimeReadModelPhaseClosureResourceTest {

	@Test
	void shouldContainOperationalReliabilityEvidenceRuntimeReadModelPhaseClosure()
			throws IOException {
		Path document = Path.of(
				"docs",
				"runtime-operational-reliability-evidence-runtime-read-model-phase-closure.md"
		);
		Path readme = Path.of("README.md");

		assertThat(Files.exists(document)).isTrue();
		assertThat(Files.exists(readme)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);
		String readmeMarkdown = Files.readString(readme, StandardCharsets.UTF_8);

		assertThat(markdown).contains(
				"# Runtime Operational Reliability Evidence Runtime Read Model Phase Closure"
		);
		assertThat(markdown).contains("## 1. Purpose");
		assertThat(markdown).contains("## 2. Completed Scope");
		assertThat(markdown).contains("## 3. Evidence Runtime Summary Semantics");
		assertThat(markdown).contains("## 4. Evidence Runtime Resource Semantics");
		assertThat(markdown).contains("## 5. Operator-Facing Read Model");
		assertThat(markdown).contains("## 6. Payload Protection Boundary");
		assertThat(markdown).contains("## 7. Payment Safety / Uncertainty Semantics");
		assertThat(markdown).contains("## 8. Runtime Invariants");
		assertThat(markdown).contains("## 9. Deferred Scope");
		assertThat(markdown).contains("## 10. Non-Goals");
		assertThat(markdown).contains("## 11. Phase Closure Summary");

		assertThat(markdown).contains("EvidenceRuntimeSummary");
		assertThat(markdown).contains("EvidenceRuntimeSummaryBuilder");
		assertThat(markdown).contains("EvidenceRuntimeSummaryStatus");
		assertThat(markdown).contains("EvidenceRuntimeSummaryReason");
		assertThat(markdown).contains("EvidenceRuntimeSummaryView");
		assertThat(markdown).contains("EvidenceRuntimeSummaryResource");
		assertThat(markdown).contains("EvidenceRuntimeSummaryResponse");
		assertThat(markdown).contains("EvidenceRuntimeSummaryResourceStatus");
		assertThat(markdown).contains("EvidenceRuntimeSummaryResourceReason");

		assertThat(markdown).contains("evidence runtime summary는 read-only");
		assertThat(markdown).contains("resource response는 recommendation이 아님");
		assertThat(markdown).contains("resource response는 execution permission이 아님");
		assertThat(markdown).contains("resource response는 action admission이 아님");
		assertThat(markdown).contains("raw payload 노출 금지");
		assertThat(markdown).contains("vendor detail 노출 금지");
		assertThat(markdown).contains("credential/configuration 노출 금지");
		assertThat(markdown).contains(
				"payment safety state / risk / uncertainty / evidence completeness만 operator-facing 노출"
		);
		assertThat(markdown).contains(
				"adapter failure는 system failure가 아니라 evidence uncertainty"
		);
		assertThat(markdown).contains(
				"payment evidence integrity missing은 payment safety uncertainty"
		);
		assertThat(markdown).contains("portfolio knowledge source 수정 금지");

		assertThat(markdown).contains("WebFlux Controller");
		assertThat(markdown).contains("RouterFunction");
		assertThat(markdown).contains("persistence-backed read model");
		assertThat(markdown).contains("API authentication/authorization");
		assertThat(markdown).contains("SRE Console integration");
		assertThat(markdown).contains("streaming evidence updates");
		assertThat(markdown).contains("external observability dashboard integration");

		assertThat(readmeMarkdown).contains(
				"### Runtime Operational Reliability Evidence Runtime Read Model Phase Closure"
		);
		assertThat(readmeMarkdown).contains(
				"docs/runtime-operational-reliability-evidence-runtime-read-model-phase-closure.md"
		);
	}
}
