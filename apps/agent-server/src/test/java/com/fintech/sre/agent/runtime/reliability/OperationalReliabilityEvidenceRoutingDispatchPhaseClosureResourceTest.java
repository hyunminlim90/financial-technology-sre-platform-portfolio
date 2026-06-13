package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class OperationalReliabilityEvidenceRoutingDispatchPhaseClosureResourceTest {

	@Test
	void shouldContainOperationalReliabilityEvidenceRoutingDispatchPhaseClosure()
			throws IOException {
		Path document = Path.of(
				"docs",
				"runtime-operational-reliability-evidence-routing-dispatch-phase-closure.md"
		);
		Path readme = Path.of("README.md");

		assertThat(Files.exists(document)).isTrue();
		assertThat(Files.exists(readme)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);
		String readmeMarkdown = Files.readString(readme, StandardCharsets.UTF_8);

		assertThat(markdown).contains(
				"# Runtime Operational Reliability Evidence Routing Dispatch Phase Closure"
		);
		assertThat(markdown).contains("## 1. Purpose");
		assertThat(markdown).contains("## 2. Completed Scope");
		assertThat(markdown).contains("## 3. Registry Semantics");
		assertThat(markdown).contains("## 4. Selection Semantics");
		assertThat(markdown).contains("## 5. Query Routing Semantics");
		assertThat(markdown).contains("## 6. Routing Plan Semantics");
		assertThat(markdown).contains("## 7. Dispatch Contract Semantics");
		assertThat(markdown).contains("## 8. Payment Consistency Routing Rule");
		assertThat(markdown).contains("## 9. Runtime Invariants");
		assertThat(markdown).contains("## 10. Deferred Scope");
		assertThat(markdown).contains("## 11. Non-Goals");
		assertThat(markdown).contains("## 12. Phase Closure Summary");

		assertThat(markdown).contains("EvidenceAdapterRegistry");
		assertThat(markdown).contains("EvidenceAdapterRegistration");
		assertThat(markdown).contains("EvidenceAdapterDescriptor");
		assertThat(markdown).contains("EvidenceAdapterAvailability");
		assertThat(markdown).contains("EvidenceAdapterRejectionReason");
		assertThat(markdown).contains("EvidenceAdapterSelector");
		assertThat(markdown).contains("EvidenceAdapterSelection");
		assertThat(markdown).contains("EvidenceAdapterSelectionPolicy");
		assertThat(markdown).contains("EvidenceAdapterSelectionRejectionReason");
		assertThat(markdown).contains("EvidenceAdapterSelectionScope");
		assertThat(markdown).contains("EvidenceQueryRouter");
		assertThat(markdown).contains("EvidenceQueryRoute");
		assertThat(markdown).contains("EvidenceQueryRoutingDecision");
		assertThat(markdown).contains("EvidenceQueryRoutingRejectionReason");
		assertThat(markdown).contains("EvidenceQueryRoutingScope");
		assertThat(markdown).contains("EvidenceRoutingPlan");
		assertThat(markdown).contains("EvidenceRoutingPlanBuilder");
		assertThat(markdown).contains("EvidenceRoutingPlanStatus");
		assertThat(markdown).contains("EvidenceRoutingPlanRejectionReason");
		assertThat(markdown).contains("EvidenceRoutingPlanScope");
		assertThat(markdown).contains("EvidenceDispatchContract");
		assertThat(markdown).contains("EvidenceDispatchRequest");
		assertThat(markdown).contains("EvidenceDispatchResult");
		assertThat(markdown).contains("EvidenceDispatchStatus");
		assertThat(markdown).contains("EvidenceDispatchRejectionReason");

		assertThat(markdown).contains("registry는 discovery-only");
		assertThat(markdown).contains("selector는 adapter 실행자가 아님");
		assertThat(markdown).contains("router는 query 실행자가 아님");
		assertThat(markdown).contains("routing plan은 semantic metadata");
		assertThat(markdown).contains("dispatch contract는 actual executor가 아님");
		assertThat(markdown).contains("accepted route/plan/dispatch != execution permission");
		assertThat(markdown).contains("payment consistency route/dispatch에는 payment-supporting adapter 필수");
		assertThat(markdown).contains("unavailable adapter route 금지");
		assertThat(markdown).contains("deprecated adapter는 restricted route/plan만 허용");
		assertThat(markdown).contains("unknown adapter는 uncertain route/plan만 허용");
		assertThat(markdown).contains("adapter/routing/dispatch failure는 system failure가 아니라 evidence uncertainty");
		assertThat(markdown).contains("raw credential/configuration 노출 금지");
		assertThat(markdown).contains("recommendation authority 없음");
		assertThat(markdown).contains("execution authority 없음");
		assertThat(markdown).contains("portfolio knowledge source 수정 금지");

		assertThat(markdown).contains("actual adapter invocation");
		assertThat(markdown).contains("WebClient/Reactor integration");
		assertThat(markdown).contains("adapter timeout/retry policy");
		assertThat(markdown).contains("adapter health check");
		assertThat(markdown).contains("evidence dispatch executor");
		assertThat(markdown).contains("persistent evidence store");
		assertThat(markdown).contains("production adapter configuration");
		assertThat(markdown).contains("observability authentication/authorization");

		assertThat(readmeMarkdown).contains(
				"### Runtime Operational Reliability Evidence Routing Dispatch Phase Closure"
		);
		assertThat(readmeMarkdown).contains(
				"docs/runtime-operational-reliability-evidence-routing-dispatch-phase-closure.md"
		);
	}
}
