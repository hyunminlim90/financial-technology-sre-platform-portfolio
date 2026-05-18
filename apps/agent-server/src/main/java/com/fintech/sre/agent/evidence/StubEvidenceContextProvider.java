package com.fintech.sre.agent.evidence;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.fintech.sre.agent.model.request.IncidentRecommendationRequest;

import reactor.core.publisher.Mono;

@Component
@Profile({"local", "dev", "test"})
public class StubEvidenceContextProvider implements EvidenceContextProvider {

	private final RequestEvidenceSignalExtractor requestEvidenceSignalExtractor;

	public StubEvidenceContextProvider(RequestEvidenceSignalExtractor requestEvidenceSignalExtractor) {
		this.requestEvidenceSignalExtractor = requestEvidenceSignalExtractor;
	}

	@Override
	public Mono<EvidenceContext> build(IncidentRecommendationRequest request) {
		String incidentId = request == null ? "unknown" : request.incidentId();
		List<EvidenceSignal> requestSignals = requestEvidenceSignalExtractor.extract(request);
		List<EvidenceSignal> signals = new java.util.ArrayList<>();
		signals.addAll(requestSignals);
		signals.addAll(List.of(
				new EvidenceSignal(
						"stub-latency-spike",
						EvidenceLayer.OBSERVABILITY,
						EvidenceSource.LOCAL_STUB,
						EvidenceSeverity.WARNING,
						"P99_LATENCY_HIGH",
						"Stub evidence: p95 latency spike detected.",
						"high",
						"normal",
						"local-stub"
				),
				new EvidenceSignal(
						"stub-error-rate-spike",
						EvidenceLayer.OBSERVABILITY,
						EvidenceSource.LOCAL_STUB,
						EvidenceSeverity.WARNING,
						"ERROR_RATE_HIGH",
						"Stub evidence: 5xx error rate increased.",
						"high",
						"normal",
						"local-stub"
				)
		));

		return Mono.just(new EvidenceContext(
				incidentId,
				signals,
				List.of("scenario/payment-latency-spike"),
				List.of("runbook/payment-latency-mitigation"),
				List.of(),
				List.of(),
				List.of(),
				List.of()
		));
	}
}
