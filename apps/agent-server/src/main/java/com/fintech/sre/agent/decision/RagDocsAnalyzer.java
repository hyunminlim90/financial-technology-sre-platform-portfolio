package com.fintech.sre.agent.decision;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

@Component
public class RagDocsAnalyzer {

	public Mono<DecisionCandidate> enrich(DecisionInput input, DecisionCandidate candidate) {
		List<String> causes = new ArrayList<>(candidate.mostLikelyCauses());
		List<String> notes = new ArrayList<>(candidate.reasoningNotes());

		if (input.incidentContext().evidence().logs() != null
				&& input.incidentContext().evidence().logs().stream().anyMatch(log -> log.toLowerCase().contains("timeout"))) {
			causes.add("Observed timeout pattern in log evidence");
		}

		if (input.incidentContext().evidence().traces() != null
				&& !input.incidentContext().evidence().traces().isEmpty()) {
			notes.add("Trace evidence reinforces dependency-wait hypothesis.");
		}

		if (input.ragSearchResult().protocols() != null
				&& !input.ragSearchResult().protocols().isEmpty()) {
			notes.add("Protocol knowledge referenced for explanation only; no action override applied.");
		}

		if (input.ragSearchResult().ragDocs() != null
				&& !input.ragSearchResult().ragDocs().isEmpty()) {
			notes.add("RAG docs used for deep diagnosis support only; no action override applied.");
		}

		return Mono.just(candidate.toBuilder()
				.mostLikelyCauses(causes.stream().distinct().toList())
				.reasoningNotes(notes.stream().distinct().toList())
				.build());
	}
}
