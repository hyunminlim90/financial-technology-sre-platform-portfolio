package com.fintech.sre.agent.guardrail;

import java.util.List;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.fintech.sre.agent.model.common.RecommendedAction;
import com.fintech.sre.agent.model.response.IncidentRecommendationResponse;

import reactor.core.publisher.Mono;

@Component
@Order(1)
public class ExecutionBoundaryGuardrail implements Guardrail {

	private static final List<String> BLOCKED_KEYWORDS = List.of(
			"kubectl apply",
			"kubectl scale",
			"helm upgrade",
			"terraform apply",
			"tofu apply",
			"argocd app sync",
			"delete pod",
			"restart pod",
			"execute",
			"run command"
	);

	@Override
	public Mono<IncidentRecommendationResponse> validate(IncidentRecommendationResponse response) {
		for (RecommendedAction action : response.recommendedActions()) {
			String text = action.action() == null ? "" : action.action().toLowerCase();
			for (String keyword : BLOCKED_KEYWORDS) {
				if (text.contains(keyword)) {
					return Mono.error(new GuardrailViolationException(
							"EXECUTION_BOUNDARY_VIOLATION",
							"AI는 실행 명령을 권장문 형태로 직접 제시할 수 없습니다: " + keyword
					));
				}
			}
		}

		return Mono.just(response);
	}
}
