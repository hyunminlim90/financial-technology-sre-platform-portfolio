package com.fintech.sre.agent.llm;

import reactor.core.publisher.Mono;

public interface LlmClient {

	Mono<LlmGenerationResponse> generate(LlmGenerationRequest request);
}
