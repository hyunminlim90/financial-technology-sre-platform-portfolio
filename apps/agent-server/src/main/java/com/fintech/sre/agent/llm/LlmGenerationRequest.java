package com.fintech.sre.agent.llm;

public record LlmGenerationRequest(
		String incidentId,
		String systemPrompt,
		String userPrompt,
		LlmPromptContext context
) {
}
