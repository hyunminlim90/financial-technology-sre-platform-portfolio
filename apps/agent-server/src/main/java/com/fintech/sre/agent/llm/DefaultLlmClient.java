package com.fintech.sre.agent.llm;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class DefaultLlmClient implements LlmClient {

	private final LlmProviderClient providerClient;
	private final LlmResponseParser responseParser;
	private final LlmOutputValidator outputValidator;

	@Override
	public Mono<LlmGenerationResponse> generate(LlmGenerationRequest request) {
		return providerClient.complete(request.systemPrompt(), request.userPrompt())
				.map(raw -> responseParser.parse(raw, request))
				.flatMap(response -> outputValidator.validate(response, request));
	}
}
