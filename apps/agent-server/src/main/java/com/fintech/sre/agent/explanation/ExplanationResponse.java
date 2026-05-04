package com.fintech.sre.agent.explanation;

public record ExplanationResponse(
		String incidentId,
		String explanation,
		boolean rootCauseInferred,
		boolean actionDecisionMadeByLlm,
		boolean requiresHumanReview
) {
}
