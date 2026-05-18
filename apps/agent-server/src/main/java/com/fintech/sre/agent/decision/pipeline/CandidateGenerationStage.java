package com.fintech.sre.agent.decision.pipeline;

import java.util.List;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.fintech.sre.agent.decision.ConfidenceEvaluator;
import com.fintech.sre.agent.decision.DecisionCandidate;
import com.fintech.sre.agent.decision.ImprovementFilter;
import com.fintech.sre.agent.decision.PostmortemAdjuster;
import com.fintech.sre.agent.decision.PreventiveDesignEvaluator;
import com.fintech.sre.agent.decision.RagDocsAnalyzer;
import com.fintech.sre.agent.decision.RunbookCandidateSelector;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Order(20)
public class CandidateGenerationStage implements DecisionPipelineStage {

	private final RunbookCandidateSelector runbookCandidateSelector;
	private final ImprovementFilter improvementFilter;
	private final PreventiveDesignEvaluator preventiveDesignEvaluator;
	private final PostmortemAdjuster postmortemAdjuster;
	private final RagDocsAnalyzer ragDocsAnalyzer;
	private final ConfidenceEvaluator confidenceEvaluator;

	public CandidateGenerationStage(
			RunbookCandidateSelector runbookCandidateSelector,
			ImprovementFilter improvementFilter,
			PreventiveDesignEvaluator preventiveDesignEvaluator,
			PostmortemAdjuster postmortemAdjuster,
			RagDocsAnalyzer ragDocsAnalyzer,
			ConfidenceEvaluator confidenceEvaluator
	) {
		this.runbookCandidateSelector = runbookCandidateSelector;
		this.improvementFilter = improvementFilter;
		this.preventiveDesignEvaluator = preventiveDesignEvaluator;
		this.postmortemAdjuster = postmortemAdjuster;
		this.ragDocsAnalyzer = ragDocsAnalyzer;
		this.confidenceEvaluator = confidenceEvaluator;
	}

	@Override
	public Mono<DecisionContext> execute(DecisionContext context) {
		Mono<List<DecisionCandidate>> generated = runbookCandidateSelector.select(
				context.request(),
				context.evidenceContext()
		);

		if (context.input() == null) {
			return generated.map(candidates -> candidates.stream()
					.map(candidate -> candidate.withEvidenceContext(context.evidenceContext()))
					.toList()).map(context::withCandidates);
		}

		return generated.flatMapMany(Flux::fromIterable)
				.map(candidate -> candidate.withEvidenceContext(context.evidenceContext()))
				.flatMap(candidate -> improve(context, candidate))
				.collectList()
				.map(context::withCandidates);
	}

	private Mono<DecisionCandidate> improve(
			DecisionContext context,
			DecisionCandidate candidate
	) {
		return Mono.just(candidate)
				.flatMap(current -> improvementFilter.apply(context.input(), current))
				.flatMap(current -> preventiveDesignEvaluator.apply(context.input(), current))
				.flatMap(current -> postmortemAdjuster.apply(context.input(), current))
				.flatMap(current -> ragDocsAnalyzer.enrich(context.input(), current))
				.flatMap(current -> confidenceEvaluator.evaluate(context.input(), current));
	}
}
