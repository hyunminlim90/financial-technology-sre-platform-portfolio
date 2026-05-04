package com.fintech.sre.agent.actionlog.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.sre.agent.actionlog.entity.IncidentRecommendationEntity;
import com.fintech.sre.agent.actionlog.model.RecommendationLog;
import com.fintech.sre.agent.actionlog.entity.RecommendationActionEntity;
import com.fintech.sre.agent.actionlog.repository.IncidentRecommendationRepository;
import com.fintech.sre.agent.actionlog.repository.RecommendationActionRepository;
import com.fintech.sre.agent.model.common.ForbiddenAction;
import com.fintech.sre.agent.model.common.RecommendedAction;
import com.fintech.sre.agent.model.request.IncidentAnalyzeRequest;
import com.fintech.sre.agent.model.request.RecommendationHistory;
import com.fintech.sre.agent.model.response.IncidentRecommendationResponse;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class RecommendationLogService {

	private final IncidentRecommendationRepository incidentRecommendationRepository;
	private final RecommendationActionRepository recommendationActionRepository;
	private final ObjectMapper objectMapper;

	public Mono<Void> saveRecommendation(
			IncidentAnalyzeRequest request,
			IncidentRecommendationResponse response
	) {
		return Mono.fromSupplier(() -> {
			String recommendationId = generateRecommendationId();
			incidentRecommendationRepository.save(IncidentRecommendationEntity.builder()
					.incidentId(request.incidentId())
					.recommendationId(recommendationId)
					.alertName(request.alertName())
					.service(request.service())
					.environment(request.environment())
					.failureMode(response.incidentSummary() == null ? null : response.incidentSummary().failureMode())
					.severity(response.incidentSummary() == null || response.incidentSummary().severity() == null
							? null : response.incidentSummary().severity().name())
					.impactScope(response.incidentSummary() == null || response.incidentSummary().impactScope() == null
							? null : response.incidentSummary().impactScope().name())
					.confidenceLevel(response.confidenceLevel() == null ? null : response.confidenceLevel().name())
					.summary(response.incidentSummary() == null ? null : response.incidentSummary().failureMode())
					.rawRequest(toJson(request))
					.rawResponse(toJson(response))
					.humanApprovalRequired(response.humanApprovalRequired())
					.createdAt(Instant.now())
					.build());

			if (response.recommendedActions() != null) {
				for (RecommendedAction action : response.recommendedActions()) {
					recommendationActionRepository.save(RecommendationActionEntity.builder()
							.recommendationId(recommendationId)
							.incidentId(request.incidentId())
							.step(action.step())
							.action(action.action())
							.expectedEffect(action.expectedEffect())
							.risk(action.risk())
							.rollbackPlan(action.rollbackPlan())
							.verification(action.verification())
							.source(action.source() == null ? null : action.source().name())
							.riskLevel(inferRiskLevel(action))
							.requiresHumanApproval(action.requiresHumanApproval())
							.status("PROPOSED")
							.createdAt(Instant.now())
							.build());
				}
			}

			return recommendationId;
		}).then();
	}

	public Mono<IncidentRecommendationResponse> record(
			IncidentAnalyzeRequest request,
			IncidentRecommendationResponse response
	) {
		return saveRecommendation(request, response)
				.thenReturn(response);
	}

	public Mono<List<RecommendationHistory>> findForPostmortem(String incidentId) {
		return Mono.fromSupplier(() -> incidentRecommendationRepository.findByIncidentId(incidentId).stream()
				.map(this::toRecommendationHistory)
				.toList());
	}

	public java.util.List<RecommendationLog> findLogs(String incidentId) {
		return incidentRecommendationRepository.findByIncidentId(incidentId).stream()
				.map(this::toRecommendationLog)
				.toList();
	}

	private String generateRecommendationId() {
		return "REC-" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID().toString().substring(0, 8);
	}

	private String toJson(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("Failed to serialize action log payload", exception);
		}
	}

	private String inferRiskLevel(RecommendedAction action) {
		String risk = action.risk() == null ? "" : action.risk().toLowerCase();
		if (risk.contains("duplicate payment") || risk.contains("high")) {
			return "HIGH";
		}
		if (risk.contains("throughput") || risk.contains("cost")) {
			return "MEDIUM";
		}
		return "LOW";
	}

	private RecommendationHistory toRecommendationHistory(IncidentRecommendationEntity entity) {
		RecommendationLog recommendationLog = toRecommendationLog(entity);
		return new RecommendationHistory(
				recommendationLog.createdAt(),
				recommendationLog.failureMode(),
				recommendationLog.confidenceLevel(),
				recommendationLog.recommendedActions(),
				recommendationLog.forbiddenActions(),
				recommendationLog.referencedKnowledge()
		);
	}

	private RecommendationLog toRecommendationLog(IncidentRecommendationEntity entity) {
		IncidentRecommendationResponse response = fromJson(entity.rawResponse());
		return new RecommendationLog(
				entity.recommendationId(),
				entity.alertName(),
				entity.service(),
				entity.environment(),
				entity.failureMode(),
				entity.severity(),
				entity.impactScope(),
				entity.confidenceLevel(),
				entity.createdAt(),
				recommendedActions(response),
				forbiddenActions(response),
				referencedKnowledge(response)
		);
	}

	private IncidentRecommendationResponse fromJson(String value) {
		try {
			return objectMapper.readValue(value, IncidentRecommendationResponse.class);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("Failed to deserialize recommendation action log", exception);
		}
	}

	private List<String> recommendedActions(IncidentRecommendationResponse response) {
		if (response.recommendedActions() == null) {
			return List.of();
		}
		return response.recommendedActions().stream()
				.map(RecommendedAction::action)
				.toList();
	}

	private List<String> forbiddenActions(IncidentRecommendationResponse response) {
		if (response.forbiddenActions() == null) {
			return List.of();
		}
		return response.forbiddenActions().stream()
				.map(ForbiddenAction::action)
				.toList();
	}

	private List<String> referencedKnowledge(IncidentRecommendationResponse response) {
		if (response.referencedKnowledge() == null) {
			return List.of();
		}

		List<String> references = new ArrayList<>();
		addAll(references, response.referencedKnowledge().scenarios());
		addAll(references, response.referencedKnowledge().runbooks());
		addAll(references, response.referencedKnowledge().improvements());
		addAll(references, response.referencedKnowledge().preventiveDesigns());
		addAll(references, response.referencedKnowledge().postmortems());
		addAll(references, response.referencedKnowledge().ragDocs());
		return references;
	}

	private void addAll(List<String> target, List<String> values) {
		if (values != null) {
			target.addAll(values);
		}
	}
}
