package com.fintech.sre.agent.llm;

import reactor.core.publisher.Mono;

public interface LlmProviderClient {

	Mono<String> complete(String systemPrompt, String userPrompt);
}
