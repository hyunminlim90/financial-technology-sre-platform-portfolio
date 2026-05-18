package com.fintech.sre.agent.decision.generator;

import java.util.List;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.decision.DecisionCandidate;
import com.fintech.sre.agent.evidence.EvidenceContext;
import com.fintech.sre.agent.model.request.IncidentRecommendationRequest;

import reactor.core.publisher.Mono;

@Component
public class FallbackNoActionCandidateGenerator implements CandidateGenerator {

	@Override
	public CandidateGenerationSource source() {
		return CandidateGenerationSource.FALLBACK_NO_ACTION;
	}

	@Override
	public Mono<List<DecisionCandidate>> generate(
			IncidentRecommendationRequest request,
			EvidenceContext evidenceContext
	) {
		return Mono.just(List.of(
				DecisionCandidate.noAction(
						request,
						evidenceContext,
						"NO_MATCHING_RUNBOOK_OR_KNOWLEDGE",
						"No matching scenario/runbook evidence found. Returning no-action recommendation."
				).withCandidateGenerationSource(source())
		));
	}
}
