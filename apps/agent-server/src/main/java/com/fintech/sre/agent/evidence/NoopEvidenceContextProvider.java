package com.fintech.sre.agent.evidence;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.fintech.sre.agent.model.request.IncidentRecommendationRequest;

import reactor.core.publisher.Mono;

@Component
@Profile("prod")
public class NoopEvidenceContextProvider implements EvidenceContextProvider {

	private final RequestEvidenceSignalExtractor extractor;

	public NoopEvidenceContextProvider(RequestEvidenceSignalExtractor extractor) {
		this.extractor = extractor;
	}

	@Override
	public Mono<EvidenceContext> build(IncidentRecommendationRequest request) {
		String incidentId = request == null ? "unknown" : request.incidentId();
		return Mono.just(new EvidenceContext(
				incidentId,
				extractor.extract(request),
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of()
		));
	}
}
