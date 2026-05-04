package com.fintech.sre.agent.guardrail;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.fintech.sre.agent.model.common.RecommendedAction;
import com.fintech.sre.agent.model.response.IncidentRecommendationResponse;

import reactor.core.publisher.Mono;

@Component
@Order(4)
public class RollbackVerificationGuardrail implements Guardrail {

	@Override
	public Mono<IncidentRecommendationResponse> validate(IncidentRecommendationResponse response) {
		for (RecommendedAction action : response.recommendedActions()) {
			if (action.rollbackPlan() == null || action.rollbackPlan().isBlank()) {
				return Mono.error(new GuardrailViolationException(
						"MISSING_ROLLBACK_PLAN",
						"Rollback Plan 없는 Action은 추천할 수 없습니다."
				));
			}

			if (action.verification() == null || action.verification().isEmpty()) {
				return Mono.error(new GuardrailViolationException(
						"MISSING_VERIFICATION",
						"Verification 없는 Action은 추천할 수 없습니다."
				));
			}
		}

		return Mono.just(response);
	}
}
