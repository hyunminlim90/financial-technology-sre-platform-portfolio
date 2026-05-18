package com.fintech.sre.agent.decision.pipeline;

import java.util.List;

import com.fintech.sre.agent.decision.DecisionCandidate;
import com.fintech.sre.agent.decision.DecisionInput;
import com.fintech.sre.agent.evidence.EvidenceContext;
import com.fintech.sre.agent.model.request.IncidentRecommendationRequest;
import com.fintech.sre.agent.model.response.IncidentRecommendationResponse;

public record DecisionContext(
		IncidentRecommendationRequest request,
		EvidenceContext evidenceContext,
		List<DecisionCandidate> candidates,
		DecisionCandidate selectedCandidate,
		IncidentRecommendationResponse response,
		DecisionInput input
) {
	public static DecisionContext fromRequest(
			IncidentRecommendationRequest request,
			EvidenceContext evidenceContext
	) {
		return new DecisionContext(
				request,
				evidenceContext,
				List.of(),
				null,
				null,
				null
		);
	}

	public static DecisionContext fromInput(DecisionInput input) {
		IncidentRecommendationRequest request = IncidentRecommendationRequest.from(input.incidentContext());
		return new DecisionContext(
				request,
				null,
				List.of(),
				null,
				null,
				input
		);
	}

	public DecisionContext withCandidates(List<DecisionCandidate> candidates) {
		return new DecisionContext(
				request,
				evidenceContext,
				candidates == null ? List.of() : List.copyOf(candidates),
				selectedCandidate,
				response,
				input
		);
	}

	public DecisionContext withSelectedCandidate(DecisionCandidate selectedCandidate) {
		return new DecisionContext(
				request,
				evidenceContext,
				candidates,
				selectedCandidate,
				response,
				input
		);
	}

	public DecisionContext withResponse(IncidentRecommendationResponse response) {
		return new DecisionContext(
				request,
				evidenceContext,
				candidates,
				selectedCandidate,
				response,
				input
		);
	}

	public DecisionContext withEvidenceContext(EvidenceContext evidenceContext) {
		return new DecisionContext(
				request,
				evidenceContext,
				candidates,
				selectedCandidate,
				response,
				input
		);
	}

	public DecisionContext withInput(DecisionInput input) {
		return new DecisionContext(
				request,
				evidenceContext,
				candidates,
				selectedCandidate,
				response,
				input
		);
	}
}
