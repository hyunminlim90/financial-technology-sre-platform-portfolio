package com.fintech.sre.agent.decision;

import java.util.List;
import java.util.LinkedHashMap;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.action.ActionCommand;
import com.fintech.sre.agent.action.ActionTarget;
import com.fintech.sre.agent.action.ActionType;
import com.fintech.sre.agent.action.RollbackCommand;
import com.fintech.sre.agent.action.VerificationCommand;
import com.fintech.sre.agent.evidence.EvidenceContext;
import com.fintech.sre.agent.knowledge.rag.KnowledgeContext;
import com.fintech.sre.agent.model.common.AlternativeAction;
import com.fintech.sre.agent.model.common.ConfidenceLevel;
import com.fintech.sre.agent.runbook.RunbookCandidateActionFactory;

import reactor.core.publisher.Mono;

@Component
public class RunbookCandidateSelector {

	private final RunbookCandidateActionFactory actionFactory;

	public RunbookCandidateSelector(
			RunbookCandidateActionFactory actionFactory
	) {
		this.actionFactory = actionFactory;
	}

	public Mono<DecisionCandidate> select(DecisionInput input, MatchedScenario scenario) {
		return select(input, scenario, null, input.knowledgeContext());
	}

	public Mono<DecisionCandidate> select(DecisionInput input, MatchedScenario scenario, EvidenceContext evidenceContext) {
		return select(input, scenario, evidenceContext, input.knowledgeContext());
	}

	public Mono<DecisionCandidate> select(
			DecisionInput input,
			MatchedScenario scenario,
			EvidenceContext evidenceContext,
			KnowledgeContext knowledgeContext
	) {
		List<CandidateAction> runbookActions = evidenceContext == null
				? List.of()
				: actionFactory.createCandidates(evidenceContext, input.incidentContext().environment());
		List<CandidateAction> evidenceActions = evidenceContext == null
				? List.of()
				: selectEvidenceBasedActions(input, evidenceContext);
		List<CandidateAction> actions = deduplicateByCommand(evidenceActions, runbookActions);

		return Mono.just(DecisionCandidate.builder()
				.scenario(scenario)
				.candidateActions(actions)
				.recommendedActions(actions)
				.alternativeActions(List.of(
						new AlternativeAction(
								"Temporarily shed low-priority traffic.",
								"Rate limiting was selected first because it is more targeted and easier to rollback."
						)
				))
				.forbiddenActions(List.of())
				.mostLikelyCauses(List.of("Initial scenario matched from curated scenario documents"))
				.reasoningNotes(List.of(reasoningNote(scenario, knowledgeContext)))
				.confidenceLevel(ConfidenceLevel.MEDIUM)
				.build());
	}

	private List<CandidateAction> deduplicateByCommand(
			List<CandidateAction> evidenceActions,
			List<CandidateAction> runbookActions
	) {
		LinkedHashMap<String, CandidateAction> deduplicated = new LinkedHashMap<>();
		for (CandidateAction action : evidenceActions) {
			deduplicated.put(commandKey(action), action);
		}
		for (CandidateAction action : runbookActions) {
			deduplicated.putIfAbsent(commandKey(action), action);
		}
		return List.copyOf(deduplicated.values());
	}

	private List<CandidateAction> selectEvidenceBasedActions(DecisionInput input, EvidenceContext context) {
		java.util.ArrayList<CandidateAction> actions = new java.util.ArrayList<>();

		if (context.hasLatencySpike()) {
			actions.add(buildScaleOutAction(input, context));
		}

		if (context.hasErrorSpike()) {
			actions.add(buildRateLimitAction(input, context));
		}

		return actions;
	}

	private CandidateAction buildScaleOutAction(DecisionInput input, EvidenceContext context) {
		ActionTarget target = new ActionTarget(
				domain(context, input),
				input.incidentContext().service(),
				"k8s-deployment",
				input.incidentContext().service(),
				input.incidentContext().environment()
		);
		return CandidateAction.builder()
				.step(1)
				.action("Scale out payment pods")
				.command(new ActionCommand(
						"scale-out-" + safe(input.incidentContext().service()),
						ActionType.SCALE_OUT,
						target,
						true,
						new RollbackCommand("Scale down to previous replica count"),
						verificationCommands(target,
								new VerificationCommand(
										"latency.p95",
										"decreasing",
										"latency 정상화 확인"
								))
				))
				.expectedEffect("Reduce latency by distributing load")
				.risk("Temporary resource increase")
				.rollbackPlan("Scale down to previous replica count")
				.verification(List.of("latency.p95 decreasing"))
				.requiresHumanApproval(true)
				.source(ActionSource.RUNBOOK)
				.riskLevel(ActionRiskLevel.MEDIUM)
				.build();
	}

	private CandidateAction buildRateLimitAction(DecisionInput input, EvidenceContext context) {
		ActionTarget target = new ActionTarget(
				domain(context, input),
				input.incidentContext().service(),
				"policy",
				"rate-limit",
				input.incidentContext().environment()
		);
		return CandidateAction.builder()
				.step(2)
				.action("Apply controlled rate limiting to payment traffic")
				.command(new ActionCommand(
						"rate-limit-" + safe(input.incidentContext().service()),
						ActionType.RATE_LIMIT,
						target,
						true,
						new RollbackCommand("Remove rate limit"),
						verificationCommands(target,
								new VerificationCommand(
										"error.rate",
										"decreasing",
										"error 감소 확인"
								))
				))
				.expectedEffect("Reduce error amplification while protecting core payment flow")
				.risk("Some low-priority traffic may be throttled")
				.rollbackPlan("Remove rate limit")
				.verification(List.of("error.rate decreasing"))
				.requiresHumanApproval(true)
				.source(ActionSource.RUNBOOK)
				.riskLevel(ActionRiskLevel.MEDIUM)
				.build();
	}

	private String domain(EvidenceContext context, DecisionInput input) {
		if (context.tags() != null && context.tags().get("domain") != null) {
			return context.tags().get("domain");
		}
		String service = input.incidentContext().service();
		if (service != null && service.contains("payment")) {
			return "payment";
		}
		return "platform";
	}

	private String safe(String value) {
		return value == null ? "unknown" : value.replaceAll("[^a-zA-Z0-9-]", "-");
	}

	private String commandKey(CandidateAction action) {
		if (action.command() == null || action.command().type() == null) {
			return action.action();
		}
		return action.command().type().name();
	}

	private String reasoningNote(MatchedScenario scenario, KnowledgeContext knowledgeContext) {
		String scenarioId = knowledgeContext == null ? null : knowledgeContext.primaryScenarioId();
		String runbookId = knowledgeContext == null ? null : knowledgeContext.primaryRunbookId();
		return "Scenario matched via curated knowledge: %s (scenarioId=%s, runbookId=%s)".formatted(
				scenario.title(),
				scenarioId == null ? "unknown" : scenarioId,
				runbookId == null ? "unknown" : runbookId
		);
	}

	private List<VerificationCommand> verificationCommands(ActionTarget target, VerificationCommand primary) {
		if (target != null && "payment".equalsIgnoreCase(target.domain())) {
			return List.of(
					primary,
					new VerificationCommand(
							"payment.consistency",
							"stable",
							"결제 정합성 / 멱등성 / 중복 결제 여부 확인"
					)
			);
		}
		return List.of(primary);
	}
}
