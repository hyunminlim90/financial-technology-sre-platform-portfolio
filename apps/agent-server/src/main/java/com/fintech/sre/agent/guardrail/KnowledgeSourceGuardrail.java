package com.fintech.sre.agent.guardrail;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.fintech.sre.agent.model.common.ActionSource;
import com.fintech.sre.agent.model.common.RecommendedAction;
import com.fintech.sre.agent.model.response.IncidentRecommendationResponse;

import reactor.core.publisher.Mono;

@Component
@Order(7)
public class KnowledgeSourceGuardrail implements Guardrail {

	@Override
	public Mono<IncidentRecommendationResponse> validate(IncidentRecommendationResponse response) {
		for (RecommendedAction action : response.recommendedActions()) {
			if (action.source() == ActionSource.RAG_DOC) {
				return Mono.error(new GuardrailViolationException(
						"RAG_DOC_ACTION_FORBIDDEN",
						"rag/docs 기반 Action은 추천할 수 없습니다."
				));
			}
		}

		return Mono.just(response);
	}
}
