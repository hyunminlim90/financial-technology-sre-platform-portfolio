package com.fintech.sre.agent.explanation;

import reactor.core.publisher.Mono;

public interface ExplanationPort {

	Mono<ExplanationResponse> explain(ExplanationRequest request);
}
