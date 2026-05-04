package com.fintech.sre.agent.guardrail;

import com.fintech.sre.agent.model.response.IncidentRecommendationResponse;

import reactor.core.publisher.Mono;

public interface Guardrail {

	Mono<IncidentRecommendationResponse> validate(IncidentRecommendationResponse response);
}
