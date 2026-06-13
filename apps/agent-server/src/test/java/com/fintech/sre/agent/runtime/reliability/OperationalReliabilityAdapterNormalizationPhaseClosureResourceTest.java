package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class OperationalReliabilityAdapterNormalizationPhaseClosureResourceTest {

	@Test
	void shouldContainOperationalReliabilityAdapterNormalizationPhaseClosure()
			throws IOException {
		Path document = Path.of(
				"docs",
				"runtime-operational-reliability-adapter-normalization-phase-closure.md"
		);
		Path readme = Path.of("README.md");

		assertThat(Files.exists(document)).isTrue();
		assertThat(Files.exists(readme)).isTrue();

		String markdown = Files.readString(document, StandardCharsets.UTF_8);
		String readmeMarkdown = Files.readString(readme, StandardCharsets.UTF_8);

		assertThat(markdown).contains(
				"# Runtime Operational Reliability Adapter Normalization Phase Closure"
		);
		assertThat(markdown).contains("## 1. Purpose");
		assertThat(markdown).contains("## 2. Completed Scope");
		assertThat(markdown).contains("## 3. Adapter Normalization Model");
		assertThat(markdown).contains("## 4. Source-Specific Boundaries");
		assertThat(markdown).contains("## 5. Raw Payload Protection");
		assertThat(markdown).contains("## 6. High-Cardinality Protection");
		assertThat(markdown).contains("## 7. Payment Safety Evidence Rule");
		assertThat(markdown).contains("## 8. Adapter Failure Semantics");
		assertThat(markdown).contains("## 9. Runtime Invariants");
		assertThat(markdown).contains("## 10. Deferred Scope");
		assertThat(markdown).contains("## 11. Non-Goals");
		assertThat(markdown).contains("## 12. Phase Closure Summary");

		assertThat(markdown).contains("PrometheusEvidenceAdapterContract");
		assertThat(markdown).contains("PrometheusEvidenceQuery");
		assertThat(markdown).contains("PrometheusEvidenceMapping");
		assertThat(markdown).contains("PrometheusMetricSemanticType");
		assertThat(markdown).contains("PrometheusEvidenceRejectionReason");

		assertThat(markdown).contains("LokiEvidenceAdapterContract");
		assertThat(markdown).contains("LokiEvidenceQuery");
		assertThat(markdown).contains("LokiEvidenceMapping");
		assertThat(markdown).contains("LokiLogSemanticType");
		assertThat(markdown).contains("LokiEvidenceRejectionReason");

		assertThat(markdown).contains("TempoEvidenceAdapterContract");
		assertThat(markdown).contains("TempoEvidenceQuery");
		assertThat(markdown).contains("TempoEvidenceMapping");
		assertThat(markdown).contains("TempoTraceSemanticType");
		assertThat(markdown).contains("TempoEvidenceRejectionReason");

		assertThat(markdown).contains("Prometheus = METRICS only");
		assertThat(markdown).contains("Loki = LOGS only");
		assertThat(markdown).contains("Tempo = TRACES only");
		assertThat(markdown).contains("raw payload exposure 금지");
		assertThat(markdown).contains("high-cardinality identifiers 노출 금지");
		assertThat(markdown).contains("customer/payment payload, token, secret, internal IP 노출 금지");
		assertThat(markdown).contains("payment-related evidence는 sanitized consistency metadata 없으면 safety evidence 승격 금지");
		assertThat(markdown).contains("adapter failure != system failure");
		assertThat(markdown).contains("all adapter output must become normalized EvidenceSignal");
		assertThat(markdown).contains("adapter contracts have no recommendation authority");
		assertThat(markdown).contains("adapter contracts have no execution authority");
		assertThat(markdown).contains("adapter contracts do not mutate portfolio knowledge source");

		assertThat(markdown).contains("actual Prometheus HTTP adapter");
		assertThat(markdown).contains("actual Loki HTTP adapter");
		assertThat(markdown).contains("actual Tempo / OpenTelemetry adapter");
		assertThat(markdown).contains("WebClient / Reactor integration");
		assertThat(markdown).contains("query timeout / retry policy");
		assertThat(markdown).contains("adapter health check");
		assertThat(markdown).contains("persistent evidence storage");
		assertThat(markdown).contains("adapter configuration management");
		assertThat(markdown).contains("production observability authentication");

		assertThat(readmeMarkdown).contains(
				"### Runtime Operational Reliability Adapter Normalization Phase Closure"
		);
		assertThat(readmeMarkdown).contains(
				"docs/runtime-operational-reliability-adapter-normalization-phase-closure.md"
		);
	}
}
