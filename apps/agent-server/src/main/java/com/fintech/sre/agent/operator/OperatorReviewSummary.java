package com.fintech.sre.agent.operator;

import java.util.List;

import com.fintech.sre.agent.actionlog.ActionLogResponse;
import com.fintech.sre.agent.decision.report.DecisionReportResponse;
import com.fintech.sre.agent.improvement.ImprovementCandidateResponse;
import com.fintech.sre.agent.knowledge.KnowledgeUpdateReviewResponse;

public record OperatorReviewSummary(
		String incidentId,
		List<ActionLogResponse> actionLogs,
		List<DecisionReportResponse> decisionReports,
		List<ImprovementCandidateResponse> improvementCandidates,
		List<KnowledgeUpdateReviewResponse> knowledgeUpdateReviews
) {
}
