package com.fintech.sre.agent.decision.pipeline;

import reactor.core.publisher.Mono;

public interface DecisionPipelineStage {

	Mono<DecisionContext> execute(DecisionContext context);

	default String name() {
		return getClass().getSimpleName();
	}
}
