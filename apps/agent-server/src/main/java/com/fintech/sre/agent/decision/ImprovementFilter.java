package com.fintech.sre.agent.decision;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.action.ActionType;
import com.fintech.sre.agent.model.common.ForbiddenAction;
import com.fintech.sre.agent.model.request.MetricsSnapshot;
import com.fintech.sre.agent.rag.RagSearchResult;

import reactor.core.publisher.Mono;

@Component
public class ImprovementFilter {

	public Mono<DecisionCandidate> apply(DecisionInput input, DecisionCandidate candidate) {
		RagSearchResult result = input.ragSearchResult();
		List<CandidateAction> recommended = new ArrayList<>(candidate.recommendedActions());
		List<ForbiddenAction> forbidden = new ArrayList<>(candidate.forbiddenActions());
		List<String> reasoning = new ArrayList<>(candidate.reasoningNotes());

		MetricsSnapshot snapshot = input.incidentContext().metricsSnapshot();
		boolean highRetry = snapshot != null && snapshot.retryRate() != null && snapshot.retryRate() >= 0.20;
		boolean highDbPending = snapshot != null && snapshot.dbConnectionPending() != null && snapshot.dbConnectionPending() >= 50;

		if (highRetry && highDbPending) {
			recommended.removeIf(action -> {
				boolean shouldRemove = action.command() != null
						&& action.command().type() == ActionType.SCALE_OUT;
				if (shouldRemove) {
					forbidden.add(new ForbiddenAction(
							action.action(),
							"Improvement constraint blocked scale-out because retry_rate and db_connection_pending are both elevated."
					));
				}
				return shouldRemove;
			});
			reasoning.add("Improvement rule applied from " + firstTitle(result.improvements()) + ".");
		}

		return Mono.just(candidate.toBuilder()
				.recommendedActions(recommended)
				.forbiddenActions(forbidden)
				.reasoningNotes(reasoning)
				.build());
	}

	private String firstTitle(List<com.fintech.sre.agent.rag.RagDocument> documents) {
		return documents == null || documents.isEmpty() ? "improvement knowledge" : documents.get(0).title();
	}
}
