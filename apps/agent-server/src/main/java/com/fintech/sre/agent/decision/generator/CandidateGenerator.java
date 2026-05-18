package com.fintech.sre.agent.decision.generator;

import java.util.List;

import com.fintech.sre.agent.decision.DecisionCandidate;
import com.fintech.sre.agent.evidence.EvidenceContext;
import com.fintech.sre.agent.model.request.IncidentRecommendationRequest;

import reactor.core.publisher.Mono;

public interface CandidateGenerator {

	CandidateGenerationSource source();

	Mono<List<DecisionCandidate>> generate(
			IncidentRecommendationRequest request,
			EvidenceContext evidenceContext
	);
}
