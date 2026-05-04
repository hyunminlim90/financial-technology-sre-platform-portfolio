package com.fintech.sre.agent.decision;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.model.common.AlternativeAction;
import com.fintech.sre.agent.model.common.ActionSource;
import com.fintech.sre.agent.model.common.ConfidenceLevel;
import com.fintech.sre.agent.model.common.ForbiddenAction;
import com.fintech.sre.agent.model.common.IncidentSummary;
import com.fintech.sre.agent.model.common.MostLikelyCause;
import com.fintech.sre.agent.model.common.RecommendedAction;
import com.fintech.sre.agent.model.common.ReferencedKnowledge;
import com.fintech.sre.agent.model.response.IncidentRecommendationResponse;
import com.fintech.sre.agent.rag.RagDocument;

import reactor.core.publisher.Mono;

@Component
public class RecommendationAssembler {

	public Mono<IncidentRecommendationResponse> assemble(DecisionInput input, DecisionCandidate candidate) {
		List<ForbiddenAction> forbiddenActions = new ArrayList<>(candidate.forbiddenActions());

		List<RecommendedAction> recommendedActions = candidate.recommendedActions().stream()
				.filter(action -> allowAction(candidate.confidenceLevel(), action, forbiddenActions))
				.map(this::toRecommendedAction)
				.toList();

		List<MostLikelyCause> mostLikelyCauses = candidate.mostLikelyCauses().stream()
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
				candidate.alternativeActions(),
				forbiddenActions,
				candidate.confidenceLevel(),
				true,
				toReferencedKnowledge(input)
		));
	}

	private boolean allowAction(
			ConfidenceLevel confidenceLevel,
			CandidateAction action,
			List<ForbiddenAction> forbiddenActions
	) {
		if (action.source() == null) {
			forbiddenActions.add(new ForbiddenAction(action.action(), "Unknown action source is not allowed."));
			return false;
		}

		if (confidenceLevel == ConfidenceLevel.LOW && action.riskLevel() == ActionRiskLevel.HIGH) {
			forbiddenActions.add(new ForbiddenAction(action.action(), "High-risk action is blocked when confidence is LOW."));
			return false;
		}

		if (action.risk() != null && action.risk().toLowerCase().contains("duplicate payment")) {
			forbiddenActions.add(new ForbiddenAction(action.action(), "Duplicate payment risk action is forbidden."));
			return false;
		}

		return true;
	}

	private RecommendedAction toRecommendedAction(CandidateAction action) {
		return new RecommendedAction(
				action.step(),
				action.action(),
				action.command(),
				action.expectedEffect(),
				action.risk(),
				action.rollbackPlan(),
				action.verification(),
				action.requiresHumanApproval(),
				mapSource(action.source())
		);
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
}
