package com.fintech.sre.agent.decision;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

@Component
public class PostmortemAdjuster {

	public Mono<DecisionCandidate> apply(DecisionInput input, DecisionCandidate candidate) {
		List<String> reasoning = new ArrayList<>(candidate.reasoningNotes());
		List<CandidateAction> recommended = new ArrayList<>(candidate.recommendedActions());

		if (input.ragSearchResult().postmortems() != null && !input.ragSearchResult().postmortems().isEmpty()) {
			reasoning.add("Recent postmortem reviewed: " + input.ragSearchResult().postmortems().get(0).title());
			recommended = recommended.stream()
					.sorted((a, b) -> Boolean.compare(a.action().contains("rate limiting"), b.action().contains("rate limiting")) * -1)
					.toList();
		}

		return Mono.just(candidate.toBuilder()
				.recommendedActions(recommended)
				.reasoningNotes(reasoning)
				.build());
	}
}
