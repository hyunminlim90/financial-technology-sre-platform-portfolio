package com.fintech.sre.agent.llm;

import com.fintech.sre.agent.model.response.IncidentRecommendationResponse;

public record LlmGenerationResponse(
		String rawOutput,
		IncidentRecommendationResponse recommendation
) {
}
