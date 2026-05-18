package com.fintech.sre.agent.evidence;

import com.fintech.sre.agent.model.request.IncidentRecommendationRequest;

import reactor.core.publisher.Mono;

public interface EvidenceContextProvider {

	Mono<EvidenceContext> build(IncidentRecommendationRequest request);
}
