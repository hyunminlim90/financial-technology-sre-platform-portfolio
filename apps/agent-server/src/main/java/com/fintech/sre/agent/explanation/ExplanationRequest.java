package com.fintech.sre.agent.explanation;

import com.fintech.sre.agent.decision.report.DecisionReport;

public record ExplanationRequest(
		String incidentId,
		DecisionReport decisionReport,
		String operatorQuestion
) {
}
