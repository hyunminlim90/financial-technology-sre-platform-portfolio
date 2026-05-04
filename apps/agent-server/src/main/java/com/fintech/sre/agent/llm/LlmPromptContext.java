package com.fintech.sre.agent.llm;

import com.fintech.sre.agent.model.response.IncidentRecommendationResponse;

public record LlmPromptContext(
		IncidentRecommendationResponse decisionResponse
) {

	public static LlmPromptContext from(IncidentRecommendationResponse response) {
		return new LlmPromptContext(response);
	}
}
