package com.fintech.sre.agent.llm;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.sre.agent.model.response.IncidentRecommendationResponse;
import com.fintech.sre.agent.model.response.PostmortemDraftResponse;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LlmResponseParser {

	private final ObjectMapper objectMapper;

	public LlmGenerationResponse parse(String raw, LlmGenerationRequest request) {
		try {
			IncidentRecommendationResponse response =
					objectMapper.readValue(raw, IncidentRecommendationResponse.class);
			return new LlmGenerationResponse(raw, response);
		} catch (Exception exception) {
			throw new IllegalArgumentException(
					"LLM output is not valid IncidentRecommendationResponse JSON",
					exception
			);
		}
	}

	public PostmortemDraftResponse parsePostmortem(String incidentId, String response) {
		return new PostmortemDraftResponse(
				incidentId,
				"DRAFT_CREATED",
				null,
				null,
				null,
				java.util.List.of(),
				java.util.List.of(),
				null,
				true,
				java.util.List.of(response)
		);
	}
}
