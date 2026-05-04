package com.fintech.sre.agent.explanation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

@Component
@ConditionalOnProperty(
		name = "agent.explanation.provider",
		havingValue = "stub",
		matchIfMissing = true
)
public class StubExplanationAdapter implements ExplanationPort {

	@Override
	public Mono<ExplanationResponse> explain(ExplanationRequest request) {
		return Mono.just(new ExplanationResponse(
				request.incidentId(),
				"""
				이 설명은 Stub Explanation입니다.

				DecisionEngine이 추천 후보를 만들고, PolicyEngine/Guardrail이 안전성을 검증했습니다.
				LLM은 Action을 결정하지 않았고, Root Cause도 확정하지 않았습니다.
				실행 전 Human Review가 필요합니다.
				""",
				false,
				false,
				true
		));
	}
}
