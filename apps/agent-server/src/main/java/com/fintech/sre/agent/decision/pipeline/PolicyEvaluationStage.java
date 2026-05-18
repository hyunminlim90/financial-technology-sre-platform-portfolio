package com.fintech.sre.agent.decision.pipeline;

import java.util.ArrayList;
import java.util.List;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.fintech.sre.agent.decision.CandidateAction;
import com.fintech.sre.agent.decision.DecisionCandidate;
import com.fintech.sre.agent.model.common.ForbiddenAction;
import com.fintech.sre.agent.policy.PolicyEngine;
import com.fintech.sre.agent.policy.PolicyEvaluationResult;
import com.fintech.sre.agent.policy.PolicySeverity;
import com.fintech.sre.agent.policy.PolicyViolation;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Order(30)
public class PolicyEvaluationStage implements DecisionPipelineStage {

	private final PolicyEngine policyEngine;

	public PolicyEvaluationStage(PolicyEngine policyEngine) {
		this.policyEngine = policyEngine;
	}

	@Override
	public Mono<DecisionContext> execute(DecisionContext context) {
		return Flux.fromIterable(context.candidates())
				.flatMap(candidate -> applyPolicy(candidate, context.evidenceContext()))
				.collectList()
				.map(context::withCandidates);
	}

	private Mono<DecisionCandidate> applyPolicy(
			DecisionCandidate candidate,
			com.fintech.sre.agent.evidence.EvidenceContext evidenceContext
	) {
		if (candidate.recommendedActions() == null || candidate.recommendedActions().isEmpty()) {
			return Mono.just(candidate);
		}

		return Flux.fromIterable(candidate.recommendedActions())
				.flatMap(action -> policyEngine.evaluate(action.command(), evidenceContext)
						.map(result -> new CandidatePolicyResult(action, result)))
				.collectList()
				.map(results -> {
					List<CandidateAction> allowed = new ArrayList<>();
					List<ForbiddenAction> forbidden = new ArrayList<>(candidate.forbiddenActions());
					List<String> reasoning = new ArrayList<>(candidate.reasoningNotes());
					List<PolicyViolation> violations = new ArrayList<>();

					for (CandidatePolicyResult result : results) {
						violations.addAll(result.evaluation().violations());
						if (result.evaluation().allowed()) {
							allowed.add(result.action());
							continue;
						}
						forbidden.addAll(toForbiddenActions(candidate, result.action(), result.evaluation()));
					}

					if (allowed.isEmpty()) {
						PolicyEvaluationResult aggregateResult = violations.stream()
								.anyMatch(violation -> violation.severity() == PolicySeverity.BLOCKING)
								? PolicyEvaluationResult.deny(violations)
								: new PolicyEvaluationResult(
										com.fintech.sre.agent.policy.PolicyDecision.ALLOW,
										violations
								);
						reasoning.add("No safe action available after policy engine evaluation.");
						return candidate.rejectByPolicy(aggregateResult).toBuilder()
								.forbiddenActions(forbidden)
								.reasoningNotes(reasoning)
								.build();
					}

					PolicyEvaluationResult aggregateResult = new PolicyEvaluationResult(
							com.fintech.sre.agent.policy.PolicyDecision.ALLOW,
							violations
					);

					return candidate.toBuilder()
							.recommendedActions(allowed)
							.forbiddenActions(forbidden)
							.reasoningNotes(reasoning)
							.policyEvaluationResult(aggregateResult)
							.build();
				});
	}

	private List<ForbiddenAction> toForbiddenActions(
			DecisionCandidate candidate,
			CandidateAction action,
			PolicyEvaluationResult result
	) {
		return result.violations().stream()
				.map(violation -> new ForbiddenAction(
						action.command() == null
								? "unknown"
								: action.command().humanReadableDescription(),
						violation.message(),
						candidate.candidateGenerationSource().name(),
						null,
						List.of(),
						null,
						violation.code()
				))
				.toList();
	}

	private record CandidatePolicyResult(
			CandidateAction action,
			PolicyEvaluationResult evaluation
	) {
	}
}
