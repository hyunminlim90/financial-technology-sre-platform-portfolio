package com.fintech.sre.agent.decision;

import java.util.List;

import com.fintech.sre.agent.model.common.AlternativeAction;
import com.fintech.sre.agent.model.common.ConfidenceLevel;
import com.fintech.sre.agent.model.common.ForbiddenAction;

import lombok.Builder;

@Builder(toBuilder = true)
public record DecisionCandidate(
		MatchedScenario scenario,
		List<CandidateAction> candidateActions,
		List<CandidateAction> recommendedActions,
		List<AlternativeAction> alternativeActions,
		List<ForbiddenAction> forbiddenActions,
		List<String> mostLikelyCauses,
		List<String> reasoningNotes,
		ConfidenceLevel confidenceLevel
) {
}
