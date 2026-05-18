package com.fintech.sre.agent.decision.pipeline;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.fintech.sre.agent.decision.RecommendationAssembler;
import com.fintech.sre.agent.decision.report.DecisionReportService;

import reactor.core.publisher.Mono;

@Component
@Order(60)
public class RecommendationAssemblyStage implements DecisionPipelineStage {

	private final RecommendationAssembler recommendationAssembler;
	private final DecisionReportService decisionReportService;

	public RecommendationAssemblyStage(
			RecommendationAssembler recommendationAssembler,
			DecisionReportService decisionReportService
	) {
		this.recommendationAssembler = recommendationAssembler;
		this.decisionReportService = decisionReportService;
	}

	@Override
	public Mono<DecisionContext> execute(DecisionContext context) {
		return recommendationAssembler.assemble(context)
				.flatMap(response -> context.input() == null
						? Mono.just(response)
						: decisionReportService.createReport(
								context.input().incidentContext().incidentId(),
								context.input().knowledgeContext() == null ? null : context.input().knowledgeContext().primaryScenarioId(),
								context.input().knowledgeContext() == null ? null : context.input().knowledgeContext().primaryRunbookId(),
								context.evidenceContext(),
								context.selectedCandidate() == null ? java.util.List.of() : context.selectedCandidate().candidateActions(),
								context.selectedCandidate() == null ? java.util.List.of() : context.selectedCandidate().recommendedActions(),
								context.input().knowledgeLayeringIssues()
						).thenReturn(response))
				.map(context::withResponse);
	}
}
