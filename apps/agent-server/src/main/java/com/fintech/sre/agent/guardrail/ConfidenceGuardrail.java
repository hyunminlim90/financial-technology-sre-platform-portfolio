package com.fintech.sre.agent.guardrail;

import java.util.List;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.fintech.sre.agent.model.common.ConfidenceLevel;
import com.fintech.sre.agent.model.common.RecommendedAction;
import com.fintech.sre.agent.model.response.IncidentRecommendationResponse;

import reactor.core.publisher.Mono;

@Component
@Order(5)
public class ConfidenceGuardrail implements Guardrail {

	private static final List<String> HIGH_RISK_ACTIONS = List.of(
			"scale-out",
			"scale-in",
			"retry",
			"timeout",
			"circuit breaker",
			"connection pool",
			"db setting",
			"redis setting",
			"traffic routing"
	);

	@Override
	public Mono<IncidentRecommendationResponse> validate(IncidentRecommendationResponse response) {
		if (response.confidenceLevel() != ConfidenceLevel.LOW) {
			return Mono.just(response);
		}

		for (RecommendedAction action : response.recommendedActions()) {
			String text = action.action().toLowerCase();
			for (String keyword : HIGH_RISK_ACTIONS) {
				if (text.contains(keyword)) {
					return Mono.error(new GuardrailViolationException(
							"LOW_CONFIDENCE_RISKY_ACTION",
							"LOW confidence 상태에서는 위험 Action을 추천할 수 없습니다: " + keyword
					));
				}
			}
		}

		return Mono.just(response);
	}
}
