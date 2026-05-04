package com.fintech.sre.agent.decision;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.model.common.ConfidenceLevel;

import reactor.core.publisher.Mono;

@Component
public class ConfidenceEvaluator {

	public Mono<DecisionCandidate> evaluate(DecisionInput input, DecisionCandidate candidate) {
		ConfidenceLevel level;

		if (input.incidentContext().availableEvidenceSources() < 2) {
			level = ConfidenceLevel.LOW;
		} else if (input.ragSearchResult().runbooks() != null
				&& !input.ragSearchResult().runbooks().isEmpty()
				&& ((input.ragSearchResult().improvements() != null && !input.ragSearchResult().improvements().isEmpty())
				|| (input.ragSearchResult().postmortems() != null && !input.ragSearchResult().postmortems().isEmpty()))) {
			level = ConfidenceLevel.HIGH;
		} else {
			level = ConfidenceLevel.MEDIUM;
		}

		return Mono.just(candidate.toBuilder()
				.confidenceLevel(level)
				.build());
	}
}
