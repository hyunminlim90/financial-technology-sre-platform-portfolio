package com.fintech.sre.agent.llm;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.model.common.ForbiddenAction;
import com.fintech.sre.agent.model.common.RecommendedAction;
import com.fintech.sre.agent.model.common.ActionSource;
import com.fintech.sre.agent.model.response.IncidentRecommendationResponse;

import reactor.core.publisher.Mono;

@Component
public class LlmOutputValidator {

	public Mono<LlmGenerationResponse> validate(LlmGenerationResponse response, LlmGenerationRequest request) {
		var recommendation = response.recommendation();
		if (recommendation.recommendedActions() == null) {
			return Mono.error(new IllegalStateException("recommendedActions must not be null"));
		}

		if (!Boolean.TRUE.equals(recommendation.humanApprovalRequired())) {
			return Mono.error(new IllegalStateException("humanApprovalRequired must always be true"));
		}

		IncidentRecommendationResponse decisionResponse = request.context().decisionResponse();

		Set<String> allowedActions = decisionResponse.recommendedActions().stream()
				.map(action -> action.action())
				.collect(Collectors.toSet());

		Set<String> requiredForbidden = decisionResponse.forbiddenActions().stream()
				.map(ForbiddenAction::action)
				.collect(Collectors.toSet());

		Set<String> responseForbidden = recommendation.forbiddenActions() == null
				? Set.of()
				: recommendation.forbiddenActions().stream().map(ForbiddenAction::action).collect(Collectors.toSet());

		for (RecommendedAction action : recommendation.recommendedActions()) {
			if (!allowedActions.contains(action.action())) {
				return Mono.error(new IllegalStateException("LLM introduced action not present in DecisionCandidate"));
			}
			if (action.command() == null || action.command().type() == null) {
				return Mono.error(new IllegalStateException("Every action must preserve ActionCommand"));
			}
			if (!Boolean.TRUE.equals(action.requiresHumanApproval())) {
				return Mono.error(new IllegalStateException("Every action requires human approval"));
			}
			if (action.rollbackPlan() == null || action.rollbackPlan().isBlank()) {
				return Mono.error(new IllegalStateException("Recommended action must include rollback plan"));
			}
			if (action.verification() == null || action.verification().isEmpty()) {
				return Mono.error(new IllegalStateException("Recommended action must include verification"));
			}
			if (action.source() == ActionSource.RAG_DOC) {
				return Mono.error(new IllegalStateException("rag/docs based action is forbidden"));
			}
		}

		if (!responseForbidden.containsAll(requiredForbidden)) {
			return Mono.error(new IllegalStateException("LLM output must preserve forbidden actions"));
		}

		return Mono.just(response);
	}
}
