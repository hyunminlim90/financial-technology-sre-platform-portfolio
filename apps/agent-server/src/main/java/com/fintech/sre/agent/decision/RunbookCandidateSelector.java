package com.fintech.sre.agent.decision;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.decision.generator.CandidateGenerationSource;
import com.fintech.sre.agent.decision.generator.CandidateGenerator;
import com.fintech.sre.agent.evidence.EvidenceContext;
import com.fintech.sre.agent.model.request.IncidentRecommendationRequest;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class RunbookCandidateSelector {

	private final List<CandidateGenerator> generators;

	public RunbookCandidateSelector(List<CandidateGenerator> generators) {
		this.generators = generators == null ? List.of() : List.copyOf(generators);
	}

	public Mono<List<DecisionCandidate>> select(
			IncidentRecommendationRequest request,
			EvidenceContext evidenceContext
	) {
		return Flux.fromIterable(generators)
				.concatMap(generator -> generator.generate(request, evidenceContext)
						.onErrorReturn(List.of()))
				.flatMapIterable(candidates -> candidates)
				.filter(this::isUsableCandidate)
				.collectList()
				.map(this::deduplicateAndSort)
				.map(this::ensureFallbackIfEmpty);
	}

	private boolean isUsableCandidate(DecisionCandidate candidate) {
		return candidate != null;
	}

	private List<DecisionCandidate> deduplicateAndSort(List<DecisionCandidate> candidates) {
		return candidates.stream()
				.collect(Collectors.toMap(
						this::dedupKey,
						candidate -> candidate,
						this::pickHigherConfidence
				))
				.values()
				.stream()
				.sorted(Comparator.comparingDouble(DecisionCandidate::confidence).reversed())
				.toList();
	}

	private String dedupKey(DecisionCandidate candidate) {
		if (candidate.actionCommand() == null) {
			return "NO_ACTION:" + candidate.blockedReason();
		}

		return candidate.actionCommand().type()
				+ ":"
				+ candidate.actionCommand().target();
	}

	private DecisionCandidate pickHigherConfidence(
			DecisionCandidate left,
			DecisionCandidate right
	) {
		return left.confidence() >= right.confidence() ? left : right;
	}

	private List<DecisionCandidate> ensureFallbackIfEmpty(List<DecisionCandidate> candidates) {
		if (candidates == null || candidates.isEmpty()) {
			return List.of();
		}

		boolean hasNonFallback = candidates.stream()
				.anyMatch(candidate -> candidate.candidateGenerationSource()
						!= CandidateGenerationSource.FALLBACK_NO_ACTION);

		if (hasNonFallback) {
			return candidates.stream()
					.filter(candidate -> candidate.candidateGenerationSource()
							!= CandidateGenerationSource.FALLBACK_NO_ACTION)
					.toList();
		}

		return candidates;
	}
}
