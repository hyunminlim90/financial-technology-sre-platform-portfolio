package com.fintech.sre.agent.llm;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.model.request.PostmortemGenerateRequest;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PromptBuilder {

	private final PromptTemplateLoader templateLoader;

	public LlmGenerationRequest buildIncidentRecommendationPrompt(String incidentId, LlmPromptContext context) {
		String systemPrompt = templateLoader.load("incident-recommendation-system-prompt.md");

		String userPrompt = """
		Incident ID: %s

		Rewrite the following structured Decision Engine response
		into the required IncidentRecommendationResponse JSON format.

		You must not add new actions.
		You must not remove forbidden actions.
		You must keep human_approval_required=true.
		You must preserve rollback_plan and verification.

		Decision Engine Response:
		%s
		""".formatted(
				incidentId,
				context.decisionResponse()
		);

		return new LlmGenerationRequest(
				incidentId,
				systemPrompt,
				userPrompt,
				context
		);
	}

	public String buildPostmortemPrompt(PostmortemGenerateRequest request) {
		return """
				Generate a fintech-safe postmortem draft.

				Incident ID: %s
				Alert: %s
				Service: %s
				Operator Summary: %s
				""".formatted(
				request.incidentId(),
				request.alertName(),
				request.service(),
				request.operatorSummary()
		);
	}
}
