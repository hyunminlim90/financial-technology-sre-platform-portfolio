package com.fintech.sre.agent.decision.pipeline;

import java.util.Comparator;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.fintech.sre.agent.decision.DecisionCandidate;

import reactor.core.publisher.Mono;

@Component
@Order(40)
public class CandidateSelectionStage implements DecisionPipelineStage {

	@Override
	public Mono<DecisionContext> execute(DecisionContext context) {
		return Mono.just(context.withSelectedCandidate(
				context.candidates().stream()
						.filter(candidate -> candidate.policyEvaluationResult() == null
								|| candidate.policyEvaluationResult().allowed())
						.max(Comparator.comparingDouble(DecisionCandidate::confidence))
						.orElse(null)
		));
	}
}
