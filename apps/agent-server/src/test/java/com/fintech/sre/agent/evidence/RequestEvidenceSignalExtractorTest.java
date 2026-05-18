package com.fintech.sre.agent.evidence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.model.request.IncidentRecommendationRequest;

class RequestEvidenceSignalExtractorTest {

	private final RequestEvidenceSignalExtractor extractor =
			new RequestEvidenceSignalExtractor();

	@Test
	void shouldExtractSignalFromRequestLabels() {
		IncidentRecommendationRequest request = new IncidentRecommendationRequest(
				"incident-1",
				"HighP99Latency",
				"payment-api",
				"prod",
				"CRITICAL",
				null,
				Map.of(
						"evidenceCode", "LATENCY_SPIKE",
						"evidenceSource", "PROMETHEUS",
						"evidenceSeverity", "CRITICAL",
						"alertName", "HighP99Latency"
				),
				null,
				List.of(),
				List.of(),
				null,
				"latency high"
		);

		List<EvidenceSignal> signals = extractor.extract(request);

		assertThat(signals).hasSize(1);
		assertThat(signals.get(0).code()).isEqualTo("LATENCY_SPIKE");
		assertThat(signals.get(0).severity()).isEqualTo(EvidenceSeverity.CRITICAL);
	}
}
