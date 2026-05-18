package com.fintech.sre.agent.decision;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.decision.pipeline.DecisionContext;
import com.fintech.sre.agent.model.common.ActionSource;
import com.fintech.sre.agent.model.common.AlternativeAction;
import com.fintech.sre.agent.model.common.ConfidenceLevel;
import com.fintech.sre.agent.model.common.Evidence;
import com.fintech.sre.agent.model.common.ForbiddenAction;
import com.fintech.sre.agent.model.common.ImpactScope;
import com.fintech.sre.agent.model.common.IncidentSummary;
import com.fintech.sre.agent.model.common.MostLikelyCause;
import com.fintech.sre.agent.model.common.RecommendedAction;
import com.fintech.sre.agent.model.common.ReferencedKnowledge;
import com.fintech.sre.agent.model.common.Severity;
import com.fintech.sre.agent.model.response.EvidenceSummaryView;
import com.fintech.sre.agent.model.response.IncidentRecommendationResponse;
import com.fintech.sre.agent.model.response.PolicyDecisionView;
import com.fintech.sre.agent.model.response.PolicyViolationView;
import com.fintech.sre.agent.rag.RagDocument;

import reactor.core.publisher.Mono;

@Component
public class RecommendationAssembler {

	public Mono<IncidentRecommendationResponse> assemble(DecisionContext context) {
		if (context.selectedCandidate() == null) {
			return Mono.just(noActionResponse(context));
		}

		if (context.input() == null) {
			return Mono.just(assembleWithoutDecisionInput(context));
		}

		return assemble(context.input(), context.selectedCandidate())
				.map(response -> mergeDecisionFields(response, context.selectedCandidate()));
	}

	public Mono<IncidentRecommendationResponse> assemble(DecisionInput input, DecisionCandidate candidate) {
		List<ForbiddenAction> forbiddenActions = new ArrayList<>(safeList(candidate.forbiddenActions()));

		List<RecommendedAction> recommendedActions = safeList(candidate.recommendedActions()).stream()
				.filter(action -> allowAction(candidate.confidenceLevel(), action, forbiddenActions))
				.map(action -> toRecommendedAction(action, candidate))
				.toList();

		List<MostLikelyCause> mostLikelyCauses = safeList(candidate.mostLikelyCauses()).stream()
				.distinct()
				.map(cause -> new MostLikelyCause(cause, candidate.confidenceLevel(), summarizeReason(candidate)))
				.toList();

		return Mono.just(new IncidentRecommendationResponse(
				input.incidentContext().incidentId(),
				recommendedActions.isEmpty() ? "NO_SAFE_ACTION_AVAILABLE" : "RECOMMENDATION_CREATED",
				new IncidentSummary(
						candidate.scenario().failureMode(),
						candidate.scenario().domain(),
						input.incidentContext().service(),
						input.incidentContext().environment(),
						candidate.scenario().severity(),
						candidate.scenario().impactScope()
				),
				mostLikelyCauses,
				input.incidentContext().evidence(),
				recommendedActions,
				safeList(candidate.alternativeActions()),
				forbiddenActions,
				candidate.confidenceLevel(),
				true,
				toReferencedKnowledge(input),
				PolicyDecisionView.from(candidate.policyEvaluationResult()),
				PolicyViolationView.fromAll(candidate.policyEvaluationResult()),
				candidate.guardrailDecision() == null ? "ALLOW" : candidate.guardrailDecision(),
				candidate.blockedReason() == null ? blockedReason(forbiddenActions) : candidate.blockedReason(),
				EvidenceSummaryView.from(candidate.evidenceContext())
		));
	}

	private IncidentRecommendationResponse assembleWithoutDecisionInput(DecisionContext context) {
		DecisionCandidate candidate = context.selectedCandidate();
		List<ForbiddenAction> forbiddenActions = safeList(candidate.forbiddenActions());

		List<RecommendedAction> recommendedActions = safeList(candidate.recommendedActions()).stream()
				.map(action -> toRecommendedAction(action, candidate))
				.toList();

		List<MostLikelyCause> mostLikelyCauses = safeList(candidate.mostLikelyCauses()).stream()
				.distinct()
				.map(cause -> new MostLikelyCause(cause, candidate.confidenceLevel(), summarizeReason(candidate)))
				.toList();

		IncidentSummary summary = candidate.scenario() == null
				? requestSummary(context)
				: new IncidentSummary(
						candidate.scenario().failureMode(),
						candidate.scenario().domain(),
						context.request().service(),
						context.request().environment(),
						candidate.scenario().severity(),
						candidate.scenario().impactScope()
				);

		return new IncidentRecommendationResponse(
				context.request().incidentId(),
				recommendedActions.isEmpty() ? "NO_SAFE_ACTION_AVAILABLE" : "RECOMMENDATION_CREATED",
				summary,
				mostLikelyCauses,
				new Evidence(List.of(), List.of(), List.of()),
				recommendedActions,
				safeList(candidate.alternativeActions()),
				forbiddenActions,
				candidate.confidenceLevel() == null ? ConfidenceLevel.LOW : candidate.confidenceLevel(),
				true,
				new ReferencedKnowledge(List.of(), List.of(), List.of(), List.of(), List.of(), List.of()),
				PolicyDecisionView.from(candidate.policyEvaluationResult()),
				PolicyViolationView.fromAll(candidate.policyEvaluationResult()),
				candidate.guardrailDecision() == null ? "ALLOW" : candidate.guardrailDecision(),
				candidate.blockedReason() == null ? blockedReason(forbiddenActions) : candidate.blockedReason(),
				EvidenceSummaryView.from(candidate.evidenceContext())
		);
	}

	private IncidentRecommendationResponse noActionResponse(DecisionContext context) {
		return new IncidentRecommendationResponse(
				context.request() == null ? null : context.request().incidentId(),
				"NO_SAFE_ACTION_AVAILABLE",
				requestSummary(context),
				List.of(),
				context.input() == null ? new Evidence(List.of(), List.of(), List.of()) : context.input().incidentContext().evidence(),
				List.of(),
				List.of(),
				List.of(),
				ConfidenceLevel.LOW,
				true,
				context.input() == null
						? new ReferencedKnowledge(List.of(), List.of(), List.of(), List.of(), List.of(), List.of())
						: toReferencedKnowledge(context.input()),
				null,
				List.of(),
				null,
				"No candidate passed policy or guardrail evaluation.",
				EvidenceSummaryView.from(context.evidenceContext())
		);
	}

	private IncidentSummary requestSummary(DecisionContext context) {
		if (context.request() == null) {
			return new IncidentSummary(
					"UNKNOWN",
					"platform",
					"unknown",
					"unknown",
					Severity.SEV_2,
					ImpactScope.PARTIAL
			);
		}

		return new IncidentSummary(
				context.request().alertName() == null ? "UNKNOWN" : context.request().alertName(),
				domainHint(context.request().service()),
				context.request().service(),
				context.request().environment(),
				parseSeverity(context.request().severityHint()),
				ImpactScope.PARTIAL
		);
	}

	private IncidentRecommendationResponse mergeDecisionFields(
			IncidentRecommendationResponse response,
			DecisionCandidate candidate
	) {
		return new IncidentRecommendationResponse(
				response.incidentId(),
				response.status(),
				response.incidentSummary(),
				response.mostLikelyCauses(),
				response.evidence(),
				response.recommendedActions(),
				response.alternativeActions(),
				response.forbiddenActions(),
				response.confidenceLevel(),
				response.humanApprovalRequired(),
				response.referencedKnowledge(),
				response.policyDecision(),
				response.policyViolations(),
				candidate.guardrailDecision() == null ? response.guardrailDecision() : candidate.guardrailDecision(),
				candidate.blockedReason() == null ? response.blockedReason() : candidate.blockedReason(),
				response.evidenceSummary()
		);
	}

	private boolean allowAction(
			ConfidenceLevel confidenceLevel,
			CandidateAction action,
			List<ForbiddenAction> forbiddenActions
	) {
		if (action.source() == null) {
			forbiddenActions.add(new ForbiddenAction(
					action.action(),
					"Unknown action source is not allowed.",
					null,
					null,
					List.of(),
					null,
					"Unknown action source is not allowed."
			));
			return false;
		}

		if (confidenceLevel == ConfidenceLevel.LOW && action.riskLevel() == ActionRiskLevel.HIGH) {
			forbiddenActions.add(new ForbiddenAction(
					action.action(),
					"High-risk action is blocked when confidence is LOW.",
					null,
					null,
					List.of(),
					null,
					"High-risk action is blocked when confidence is LOW."
			));
			return false;
		}

		if (action.risk() != null && action.risk().toLowerCase().contains("duplicate payment")) {
			forbiddenActions.add(new ForbiddenAction(
					action.action(),
					"Duplicate payment risk action is forbidden.",
					null,
					null,
					List.of(),
					null,
					"Duplicate payment risk action is forbidden."
			));
			return false;
		}

		return true;
	}

	private RecommendedAction toRecommendedAction(CandidateAction action, DecisionCandidate candidate) {
		return new RecommendedAction(
				action.step(),
				action.action(),
				action.command(),
				action.expectedEffect(),
				action.risk(),
				action.rollbackPlan(),
				action.verification(),
				action.requiresHumanApproval(),
				mapSource(action.source()),
				candidate.candidateGenerationSource().name(),
				null,
				List.of(),
				"ALLOW",
				null
		);
	}

	private String blockedReason(List<ForbiddenAction> forbiddenActions) {
		if (forbiddenActions == null || forbiddenActions.isEmpty()) {
			return null;
		}
		return forbiddenActions.stream()
				.map(ForbiddenAction::reason)
				.filter(reason -> reason != null && !reason.isBlank())
				.distinct()
				.reduce((left, right) -> left + " | " + right)
				.orElse(null);
	}

	private ActionSource mapSource(com.fintech.sre.agent.decision.ActionSource source) {
		if (source == null) {
			return null;
		}
		return switch (source) {
			case RUNBOOK -> ActionSource.RUNBOOK;
			case IMPROVEMENT -> ActionSource.IMPROVEMENT;
			case PREVENTIVE_DESIGN -> ActionSource.PREVENTIVE_DESIGN;
			case POSTMORTEM -> ActionSource.POSTMORTEM;
		};
	}

	private String summarizeReason(DecisionCandidate candidate) {
		if (candidate.reasoningNotes() == null || candidate.reasoningNotes().isEmpty()) {
			return "Derived from matched scenario and curated RAG knowledge.";
		}
		return candidate.reasoningNotes().get(0);
	}

	private ReferencedKnowledge toReferencedKnowledge(DecisionInput input) {
		return new ReferencedKnowledge(
				titles(input.ragSearchResult().scenarios()),
				titles(input.ragSearchResult().runbooks()),
				titles(input.ragSearchResult().improvements()),
				titles(input.ragSearchResult().preventiveDesigns()),
				titles(input.ragSearchResult().postmortems()),
				titles(input.ragSearchResult().ragDocs())
		);
	}

	private List<String> titles(List<RagDocument> documents) {
		return documents == null ? List.of() : documents.stream().map(RagDocument::title).toList();
	}

	private String domainHint(String service) {
		if (service != null && service.toLowerCase().contains("payment")) {
			return "payment";
		}
		return "platform";
	}

	private Severity parseSeverity(String severityHint) {
		if (severityHint == null || severityHint.isBlank()) {
			return Severity.SEV_2;
		}
		return switch (severityHint.trim().toUpperCase()) {
			case "SEV_1", "SEV1" -> Severity.SEV_1;
			case "SEV_3", "SEV3" -> Severity.SEV_3;
			default -> Severity.SEV_2;
		};
	}

	private <T> List<T> safeList(List<T> values) {
		return values == null ? List.of() : values;
	}
}
