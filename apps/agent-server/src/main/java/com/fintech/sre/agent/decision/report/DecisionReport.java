package com.fintech.sre.agent.decision.report;

import java.time.Instant;
import java.util.List;

import com.fintech.sre.agent.evidence.Evidence;
import com.fintech.sre.agent.knowledge.layering.KnowledgeLayeringIssue;

public record DecisionReport(
		String id,
		String incidentId,
		String scenarioId,
		String runbookId,
		DecisionReportStatus status,
		List<Evidence> evidenceSignals,
		List<DecisionReportAction> actions,
		List<KnowledgeLayeringIssue> knowledgeLayeringIssues,
		List<String> humanReviewRequirements,
		String markdown,
		Instant createdAt,
		Instant updatedAt
) {
}
