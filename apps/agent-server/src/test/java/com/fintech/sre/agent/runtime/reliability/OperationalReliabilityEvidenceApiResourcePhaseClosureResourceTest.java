package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class OperationalReliabilityEvidenceApiResourcePhaseClosureResourceTest {

	@Test
	void shouldContainOperationalReliabilityEvidenceApiResourcePhaseClosure()
			throws IOException {
		Path document = Path.of(
				"docs",
				"runtime-operational-reliability-evidence-api-resource-phase-closure.md"
		);
		Path readme = Path.of("README.md");

		assertThat(Files.exists(document)).isTrue();
		assertThat(Files.exists(readme)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);
		String readmeMarkdown = Files.readString(readme, StandardCharsets.UTF_8);

		assertThat(markdown).contains(
				"# Runtime Operational Reliability Evidence API Resource Phase Closure"
		);
		assertThat(markdown).contains("## 1. Purpose");
		assertThat(markdown).contains("## 2. Completed Scope");
		assertThat(markdown).contains("## 3. Evidence Runtime Summary Semantics");
		assertThat(markdown).contains("## 4. Evidence Runtime Resource Semantics");
		assertThat(markdown).contains("## 5. API Boundary Semantics");
		assertThat(markdown).contains("## 6. Operator-Facing Exposure Boundary");
		assertThat(markdown).contains("## 7. Payload Protection Boundary");
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
		assertThat(markdown).contains("EvidenceRuntimeApiBoundary");
		assertThat(markdown).contains("EvidenceRuntimeApiRequest");
		assertThat(markdown).contains("EvidenceRuntimeApiResponse");
		assertThat(markdown).contains("EvidenceRuntimeApiStatus");
		assertThat(markdown).contains("EvidenceRuntimeApiRejectionReason");

		assertThat(markdown).contains(
				"evidence runtime API boundary는 actual HTTP endpoint가 아님"
		);
		assertThat(markdown).contains("WebFlux Controller / RouterFunction은 아직 없음");
		assertThat(markdown).contains("response는 read-only");
		assertThat(markdown).contains("response는 recommendation이 아님");
		assertThat(markdown).contains("response는 execution permission이 아님");
		assertThat(markdown).contains("response는 ActionCommand admission이 아님");
		assertThat(markdown).contains(
				"payment safety state / risk / uncertainty / evidence completeness / audit trusted만 operator-facing 노출"
		);
		assertThat(markdown).contains("raw payload 노출 금지");
		assertThat(markdown).contains("vendor detail 노출 금지");
		assertThat(markdown).contains("credential/configuration 노출 금지");
		assertThat(markdown).contains("untrusted audit은 explicit status로 표시");
		assertThat(markdown).contains("authentication/authorization은 deferred");
		assertThat(markdown).contains("portfolio knowledge source 수정 금지");

		assertThat(markdown).contains("WebFlux Controller");
		assertThat(markdown).contains("RouterFunction");
		assertThat(markdown).contains("API authentication/authorization");
		assertThat(markdown).contains("persistence-backed read model");
		assertThat(markdown).contains("SRE Console integration");
		assertThat(markdown).contains("streaming evidence updates");
		assertThat(markdown).contains("API rate limiting");
		assertThat(markdown).contains("Cloudflare Access / Zero Trust integration");

		assertThat(readmeMarkdown).contains(
				"### Runtime Operational Reliability Evidence API Resource Phase Closure"
		);
		assertThat(readmeMarkdown).contains(
				"docs/runtime-operational-reliability-evidence-api-resource-phase-closure.md"
		);
	}
}
