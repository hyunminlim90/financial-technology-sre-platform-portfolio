package com.fintech.sre.agent.model.response;

import java.util.List;

import com.fintech.sre.agent.model.common.AlternativeAction;
import com.fintech.sre.agent.model.common.ConfidenceLevel;
import com.fintech.sre.agent.model.common.Evidence;
import com.fintech.sre.agent.model.common.ForbiddenAction;
import com.fintech.sre.agent.model.common.IncidentSummary;
import com.fintech.sre.agent.model.common.MostLikelyCause;
import com.fintech.sre.agent.model.common.RecommendedAction;
import com.fintech.sre.agent.model.common.ReferencedKnowledge;

public record IncidentRecommendationResponse(
		String incidentId,
		String status,
		IncidentSummary incidentSummary,
		List<MostLikelyCause> mostLikelyCauses,
		Evidence evidence,
		List<RecommendedAction> recommendedActions,
		List<AlternativeAction> alternativeActions,
		List<ForbiddenAction> forbiddenActions,
		ConfidenceLevel confidenceLevel,
		Boolean humanApprovalRequired,
		ReferencedKnowledge referencedKnowledge
) {
}
