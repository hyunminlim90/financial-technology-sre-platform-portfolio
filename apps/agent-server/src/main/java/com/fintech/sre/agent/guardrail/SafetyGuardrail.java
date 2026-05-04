package com.fintech.sre.agent.guardrail;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.fintech.sre.agent.model.common.RecommendedAction;
import com.fintech.sre.agent.model.response.IncidentRecommendationResponse;

import reactor.core.publisher.Mono;

@Component
@Order(3)
public class SafetyGuardrail implements Guardrail {

	@Override
	public Mono<IncidentRecommendationResponse> validate(IncidentRecommendationResponse response) {
		if (!Boolean.TRUE.equals(response.humanApprovalRequired())) {
			return Mono.error(new GuardrailViolationException(
					"HUMAN_APPROVAL_REQUIRED",
					"모든 Recommendation은 Human Approval Required=true 여야 합니다."
			));
		}

		for (RecommendedAction action : response.recommendedActions()) {
			if (!Boolean.TRUE.equals(action.requiresHumanApproval())) {
				return Mono.error(new GuardrailViolationException(
						"ACTION_HUMAN_APPROVAL_REQUIRED",
						"모든 Action은 Human Approval이 필요합니다."
				));
			}
		}

		return Mono.just(response);
	}
}
