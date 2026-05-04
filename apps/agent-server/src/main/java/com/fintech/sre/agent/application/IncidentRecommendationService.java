package com.fintech.sre.agent.application;

import java.util.List;

import org.springframework.stereotype.Service;

import com.fintech.sre.agent.actionlog.ActionLogService;
import com.fintech.sre.agent.actionlog.service.RecommendationLogService;
import com.fintech.sre.agent.decision.DecisionInput;
import com.fintech.sre.agent.decision.DecisionEngine;
import com.fintech.sre.agent.evidence.EvidenceContextProvider;
import com.fintech.sre.agent.guardrail.GuardrailChain;
import com.fintech.sre.agent.incident.IncidentLifecycleService;
import com.fintech.sre.agent.incident.IncidentStatus;
import com.fintech.sre.agent.knowledge.layering.KnowledgeLayeringValidator;
import com.fintech.sre.agent.knowledge.rag.KnowledgeContext;
import com.fintech.sre.agent.knowledge.rag.KnowledgeContextAssembler;
import com.fintech.sre.agent.knowledge.rag.KnowledgeConsumerPolicyGuardrail;
import com.fintech.sre.agent.knowledge.rag.KnowledgeLayer;
import com.fintech.sre.agent.knowledge.rag.KnowledgeSearchClient;
import com.fintech.sre.agent.knowledge.rag.KnowledgeSearchRequest;
import com.fintech.sre.agent.model.common.IncidentContext;
import com.fintech.sre.agent.model.request.IncidentAnalyzeRequest;
import com.fintech.sre.agent.model.response.IncidentRecommendationResponse;
import com.fintech.sre.agent.observability.ObservabilityQueryService;
import com.fintech.sre.agent.rag.RagRetrievalService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class IncidentRecommendationService {

	private final IncidentRequestValidator requestValidator;
	private final ObservabilityQueryService observabilityQueryService;
	private final RagRetrievalService ragRetrievalService;
	private final KnowledgeSearchClient knowledgeSearchClient;
	private final KnowledgeContextAssembler knowledgeContextAssembler;
	private final KnowledgeConsumerPolicyGuardrail knowledgeConsumerPolicyGuardrail;
	private final KnowledgeLayeringValidator knowledgeLayeringValidator;
	private final DecisionEngine decisionEngine;
	private final GuardrailChain guardrailChain;
	private final RecommendationLogService recommendationLogService;
	private final ActionLogService actionLogService;
	private final EvidenceContextProvider evidenceContextProvider;
	private final IncidentLifecycleService incidentLifecycleService;

	public Mono<IncidentRecommendationResponse> analyze(IncidentAnalyzeRequest request) {
		return incidentLifecycleService.createIfAbsent(request.incidentId())
				.then(requestValidator.validate(request))
				.then(observabilityQueryService.enrich(request))
				.flatMap(this::retrieveKnowledge)
				.flatMap(input -> runDecisionEngine(input)
						.flatMap(guardrailChain::validate)
						.map(response -> new RecommendationEnvelope(input, response)))
				.flatMap(envelope -> incidentLifecycleService.advanceTo(
								envelope.response().incidentId(),
								IncidentStatus.RECOMMENDATION_CREATED,
								"recommendation created"
						)
						.then(recommendationLogService.saveRecommendation(request, envelope.response()))
						.then(recordActionLogs(envelope))
						.then(incidentLifecycleService.advanceTo(
								envelope.response().incidentId(),
								IncidentStatus.HUMAN_REVIEW_REQUIRED,
								"decision report and action logs created"
						))
						.thenReturn(envelope.response()));
	}

	private Mono<DecisionInput> retrieveKnowledge(IncidentContext context) {
		return Mono.zip(
						ragRetrievalService.retrieve(context),
						retrieveKnowledgeContext(context)
				)
				.map(tuple -> new DecisionInput(
						context,
						tuple.getT1(),
						tuple.getT2().context(),
						tuple.getT2().issues()
				));
	}

	private Mono<IncidentRecommendationResponse> runDecisionEngine(DecisionInput input) {
		return decisionEngine.decide(input);
	}

	private Mono<KnowledgeLayeringValidator.ValidatedKnowledgeContext> retrieveKnowledgeContext(IncidentContext context) {
		return knowledgeSearchClient.search(new KnowledgeSearchRequest(
						String.join(" ", context.keywords()),
						List.of(KnowledgeLayer.SCENARIO, KnowledgeLayer.RUNBOOK),
						List.of(
								KnowledgeLayer.POSTMORTEM,
								KnowledgeLayer.IMPROVEMENT,
								KnowledgeLayer.PREVENTIVE_DESIGN,
								KnowledgeLayer.POLICY,
								KnowledgeLayer.RAG_DOC,
								KnowledgeLayer.PROTOCOL
						),
						context.labels() == null ? java.util.Map.of() : context.labels(),
						10
				))
				.map(knowledgeContextAssembler::assemble)
				.flatMap(knowledgeConsumerPolicyGuardrail::validate)
				.flatMap(knowledgeLayeringValidator::validate);
	}

	private Mono<Void> recordActionLogs(RecommendationEnvelope envelope) {
		IncidentRecommendationResponse response = envelope.response();
		if (response.recommendedActions() == null || response.recommendedActions().isEmpty()) {
			return Mono.empty();
		}

		String scenarioId = envelope.input().knowledgeContext() == null
				? null
				: envelope.input().knowledgeContext().primaryScenarioId();
		String runbookId = envelope.input().knowledgeContext() == null
				? null
				: envelope.input().knowledgeContext().primaryRunbookId();

		return evidenceContextProvider.provide(response.incidentId())
				.flatMapMany(evidenceContext -> Flux.fromIterable(response.recommendedActions())
						.flatMap(action -> actionLogService.recordRecommendation(
								response.incidentId(),
								scenarioId,
								runbookId != null ? runbookId : action.source() == null ? null : action.source().name(),
								action,
								evidenceContext.signalNames()
						)))
				.then();
	}

	private record RecommendationEnvelope(
			DecisionInput input,
			IncidentRecommendationResponse response
	) {
	}
}
