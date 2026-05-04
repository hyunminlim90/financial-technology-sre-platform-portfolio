package com.fintech.sre.agent.decision;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

@Component
public class PreventiveDesignEvaluator {

	public Mono<DecisionCandidate> apply(DecisionInput input, DecisionCandidate candidate) {
		List<CandidateAction> recommended = new ArrayList<>(candidate.recommendedActions());
		List<String> reasoning = new ArrayList<>(candidate.reasoningNotes());

		boolean sevOneOrTwo = candidate.scenario().severity() != null
				&& candidate.scenario().severity() != com.fintech.sre.agent.model.common.Severity.SEV_3;
		boolean duplicatePaymentRisk = input.incidentContext().service().contains("payment")
				|| input.incidentContext().service().contains("checkout");

		if (sevOneOrTwo && duplicatePaymentRisk && input.ragSearchResult().preventiveDesigns() != null
				&& !input.ragSearchResult().preventiveDesigns().isEmpty()) {
			reasoning.add("Preventive design guidance applied: " + input.ragSearchResult().preventiveDesigns().get(0).title());
			recommended = recommended.stream()
					.map(action -> action.toBuilder()
							.expectedEffect(action.expectedEffect() + " Include idempotency and duplicate-payment guard review in follow-up.")
							.build())
					.toList();
		}

		return Mono.just(candidate.toBuilder()
				.recommendedActions(recommended)
				.reasoningNotes(reasoning)
				.build());
	}
}
