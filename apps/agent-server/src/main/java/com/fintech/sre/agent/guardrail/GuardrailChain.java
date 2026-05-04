package com.fintech.sre.agent.guardrail;

import java.util.List;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.model.response.IncidentRecommendationResponse;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class GuardrailChain {

	private final List<Guardrail> guardrails;

	public Mono<IncidentRecommendationResponse> validate(IncidentRecommendationResponse response) {
		Mono<IncidentRecommendationResponse> result = Mono.just(response);
		for (Guardrail guardrail : guardrails) {
			result = result.flatMap(guardrail::validate);
		}
		return result;
	}
}
