package com.fintech.sre.agent.decision;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.decision.policy.ActionPolicyEngine;
import com.fintech.sre.agent.decision.report.DecisionReportService;
import com.fintech.sre.agent.evidence.EvidenceContextProvider;
import com.fintech.sre.agent.evidence.EvidenceContext;
import com.fintech.sre.agent.evidence.EvidenceNormalizer;
import com.fintech.sre.agent.exception.NoScenarioMatchException;
import com.fintech.sre.agent.model.common.ForbiddenAction;
import com.fintech.sre.agent.model.response.IncidentRecommendationResponse;
import com.fintech.sre.agent.policy.PolicyEngine;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class DecisionEngine {

	private final ScenarioMatcher scenarioMatcher;
	private final RunbookCandidateSelector runbookCandidateSelector;
	private final ImprovementFilter improvementFilter;
	private final PreventiveDesignEvaluator preventiveDesignEvaluator;
	private final PostmortemAdjuster postmortemAdjuster;
	private final RagDocsAnalyzer ragDocsAnalyzer;
	private final ConfidenceEvaluator confidenceEvaluator;
	private final RecommendationAssembler recommendationAssembler;
	private final ActionPolicyEngine actionPolicyEngine;
	private final EvidenceContextProvider evidenceContextProvider;
	private final EvidenceNormalizer evidenceNormalizer;
	private final PolicyEngine policyEngine;
	private final DecisionReportService decisionReportService;

	public Mono<DecisionCandidate> decideCandidate(DecisionInput input) {
		return resolveEvidenceContext(input)
				.flatMap(evidenceContext -> scenarioMatcher.match(input)
						.switchIfEmpty(Mono.error(new NoScenarioMatchException("No Scenario -> No Action")))
						.flatMap(scenario -> runbookCandidateSelector.select(
										input,
										scenario,
										evidenceContext,
										input.knowledgeContext()
								)
								.map(candidate -> applyPolicy(input, candidate))
								.flatMap(candidate -> applyFintechPolicy(candidate, evidenceContext))))
				.flatMap(candidate -> improvementFilter.apply(input, candidate))
				.flatMap(candidate -> preventiveDesignEvaluator.apply(input, candidate))
				.flatMap(candidate -> postmortemAdjuster.apply(input, candidate))
				.flatMap(candidate -> ragDocsAnalyzer.enrich(input, candidate))
				.flatMap(candidate -> confidenceEvaluator.evaluate(input, candidate));
	}

	public Mono<IncidentRecommendationResponse> decide(DecisionInput input) {
		return resolveEvidenceContext(input)
				.flatMap(evidenceContext -> decideCandidate(input)
						.flatMap(candidate -> decisionReportService.createReport(
								input.incidentContext().incidentId(),
								input.knowledgeContext() == null ? null : input.knowledgeContext().primaryScenarioId(),
								input.knowledgeContext() == null ? null : input.knowledgeContext().primaryRunbookId(),
								evidenceContext,
								candidate.candidateActions(),
								candidate.recommendedActions(),
								input.knowledgeLayeringIssues()
						).thenReturn(candidate))
						.flatMap(candidate -> recommendationAssembler.assemble(input, candidate)));
	}

	private DecisionCandidate applyPolicy(DecisionInput input, DecisionCandidate candidate) {
		List<CandidateAction> allowed = new ArrayList<>();
		List<ForbiddenAction> forbiddenActions = new ArrayList<>(candidate.forbiddenActions());
		for (CandidateAction action : candidate.recommendedActions()) {
			var result = actionPolicyEngine.evaluate(input, action);
			if (result.allowed()) {
				allowed.add(action);
				continue;
			}
			String reason = result.reason() == null ? "Policy engine denied the action." : result.reason();
			forbiddenActions.add(new ForbiddenAction(action.action(), reason));
		}

		List<String> reasoningNotes = new ArrayList<>(candidate.reasoningNotes());
		if (allowed.isEmpty()) {
			reasoningNotes.add("No safe action available after policy evaluation.");
		}

		return candidate.toBuilder()
				.recommendedActions(allowed)
				.forbiddenActions(forbiddenActions)
				.reasoningNotes(reasoningNotes)
				.build();
	}

	private Mono<DecisionCandidate> applyFintechPolicy(DecisionCandidate candidate, com.fintech.sre.agent.evidence.EvidenceContext evidenceContext) {
		return Flux.fromIterable(candidate.recommendedActions())
				.flatMap(action -> policyEngine.evaluate(action.command(), evidenceContext)
						.map(result -> new CandidatePolicyResult(action, result)))
				.collectList()
				.map(results -> {
					List<CandidateAction> allowed = new ArrayList<>();
					List<ForbiddenAction> forbidden = new ArrayList<>(candidate.forbiddenActions());
					List<String> reasoning = new ArrayList<>(candidate.reasoningNotes());

					for (CandidatePolicyResult result : results) {
						if (result.evaluation().allowed()) {
							allowed.add(result.action());
							continue;
						}
						String message = result.evaluation().violations().isEmpty()
								? "Policy engine denied the action."
								: result.evaluation().violations().stream()
										.map(com.fintech.sre.agent.policy.PolicyViolation::message)
										.distinct()
										.reduce((left, right) -> left + " " + right)
										.orElse("Policy engine denied the action.");
						forbidden.add(new ForbiddenAction(result.action().action(), message));
					}

					if (allowed.isEmpty()) {
						reasoning.add("No safe action available after fintech policy evaluation.");
					}

					return candidate.toBuilder()
							.recommendedActions(allowed)
							.forbiddenActions(forbidden)
							.reasoningNotes(reasoning)
							.build();
				});
	}

	private record CandidatePolicyResult(
			CandidateAction action,
			com.fintech.sre.agent.policy.PolicyEvaluationResult evaluation
	) {
	}

	private Mono<EvidenceContext> resolveEvidenceContext(DecisionInput input) {
		return evidenceContextProvider.provide(input.incidentContext().incidentId())
				.map(provided -> evidenceNormalizer.merge(
						provided,
						evidenceNormalizer.normalize(input.incidentContext())
				));
	}
}
