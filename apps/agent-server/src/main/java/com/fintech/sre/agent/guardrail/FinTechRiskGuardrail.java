package com.fintech.sre.agent.guardrail;

import java.util.List;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.fintech.sre.agent.action.ActionType;
import com.fintech.sre.agent.model.common.RecommendedAction;
import com.fintech.sre.agent.model.response.IncidentRecommendationResponse;

import reactor.core.publisher.Mono;

@Component
@Order(6)
public class FinTechRiskGuardrail implements Guardrail {

	private static final List<String> DANGEROUS_PATTERNS = List.of(
			"disable idempotency",
			"ignore duplicate",
			"skip duplicate check",
			"force payment success",
			"bypass validation",
			"increase retry without limit"
	);

	@Override
	public Mono<IncidentRecommendationResponse> validate(IncidentRecommendationResponse response) {
		for (RecommendedAction action : response.recommendedActions()) {
			if (action.command() != null
					&& action.command().type() == ActionType.RESTART_POD
					&& action.command().target() != null
					&& "payment".equalsIgnoreCase(action.command().target().domain())) {
				return Mono.error(new GuardrailViolationException(
						"PAYMENT_RISK",
						"결제 시스템에서 pod restart 금지"
				));
			}

			String actionText = action.action() == null ? "" : action.action().toLowerCase();
			String riskText = action.risk() == null ? "" : action.risk().toLowerCase();
			String text = actionText + " " + riskText;

			for (String pattern : DANGEROUS_PATTERNS) {
				if (text.contains(pattern)) {
					return Mono.error(new GuardrailViolationException(
							"FINTECH_SAFETY_VIOLATION",
							"결제 정합성을 침해할 수 있는 Action은 추천할 수 없습니다: " + pattern
					));
				}
			}
		}

		return Mono.just(response);
	}
}
