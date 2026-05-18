package com.fintech.sre.agent.decision.pipeline;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.fintech.sre.agent.decision.RecommendationAssembler;
import com.fintech.sre.agent.guardrail.GuardrailChain;
import com.fintech.sre.agent.guardrail.GuardrailViolationException;

import reactor.core.publisher.Mono;

@Component
@Order(50)
public class GuardrailEvaluationStage implements DecisionPipelineStage {

	private final GuardrailChain guardrailChain;
	private final RecommendationAssembler recommendationAssembler;

	public GuardrailEvaluationStage(
			GuardrailChain guardrailChain,
			RecommendationAssembler recommendationAssembler
	) {
		this.guardrailChain = guardrailChain;
		this.recommendationAssembler = recommendationAssembler;
	}

	@Override
	public Mono<DecisionContext> execute(DecisionContext context) {
		if (context.selectedCandidate() == null) {
			return Mono.just(context);
		}

		return recommendationAssembler.assemble(context)
				.flatMap(guardrailChain::validate)
				.thenReturn(context)
				.onErrorResume(GuardrailViolationException.class, ex ->
						Mono.just(context.withSelectedCandidate(
								context.selectedCandidate().rejectByGuardrail(
										ex.code(),
										ex.getMessage()
								)
						))
				);
	}
}
