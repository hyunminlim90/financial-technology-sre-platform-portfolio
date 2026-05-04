package com.fintech.sre.agent.actionlog.service;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.fintech.sre.agent.actionlog.dto.RecordExecutedActionRequest;
import com.fintech.sre.agent.actionlog.entity.ExecutedActionEntity;
import com.fintech.sre.agent.actionlog.entity.RecommendationActionEntity;
import com.fintech.sre.agent.actionlog.repository.ExecutedActionRepository;
import com.fintech.sre.agent.actionlog.repository.RecommendationActionRepository;
import com.fintech.sre.agent.exception.ActionLogNotFoundException;
import com.fintech.sre.agent.model.request.ExecutedAction;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ExecutedActionService {

	private final ExecutedActionRepository executedActionRepository;
	private final RecommendationActionRepository recommendationActionRepository;

	public Mono<ExecutedActionEntity> record(String incidentId, RecordExecutedActionRequest request) {
		return Mono.fromSupplier(() -> {
			RecommendationActionEntity recommendationAction = null;
			if (request.recommendationActionId() != null) {
				recommendationAction = recommendationActionRepository.findById(request.recommendationActionId())
						.orElseThrow(() -> new ActionLogNotFoundException(
								"Recommendation action not found: " + request.recommendationActionId()
						));
				if (!incidentId.equals(recommendationAction.incidentId())) {
					throw new ActionLogNotFoundException(
							"Recommendation action does not belong to incident: " + incidentId
					);
				}
				recommendationActionRepository.save(recommendationAction.toBuilder()
						.status("EXECUTED")
						.build());
			}

			return executedActionRepository.save(ExecutedActionEntity.builder()
					.incidentId(incidentId)
					.recommendationId(resolveRecommendationId(request, recommendationAction))
					.recommendationActionId(request.recommendationActionId())
					.action(request.action())
					.executedBy(request.executedBy())
					.executedAt(request.executedAt())
					.executionMethod(request.executionMethod())
					.executionDetail(request.executionDetail())
					.expectedEffect(request.expectedEffect())
					.actualEffect(request.actualEffect())
					.rollbackPlan(request.rollbackPlan())
					.rollbackExecuted(false)
					.createdAt(Instant.now())
					.build());
		});
	}

	public Mono<java.util.List<ExecutedAction>> findForPostmortem(String incidentId) {
		return Mono.fromSupplier(() -> executedActionRepository.findByIncidentId(incidentId).stream()
				.map(entity -> {
					Integer step = entity.recommendationActionId() == null ? null
							: recommendationActionRepository.findById(entity.recommendationActionId())
									.map(RecommendationActionEntity::step)
									.orElse(null);
					return new ExecutedAction(
							step,
							entity.action(),
							entity.executedAt(),
							entity.executedBy(),
							entity.expectedEffect(),
							entity.actualEffect(),
							entity.rollbackPlan(),
							entity.rollbackExecuted(),
							java.util.List.of()
					);
				})
				.toList());
	}

	private String resolveRecommendationId(
			RecordExecutedActionRequest request,
			RecommendationActionEntity recommendationAction
	) {
		if (request.recommendationId() != null && !request.recommendationId().isBlank()) {
			return request.recommendationId();
		}
		return recommendationAction == null ? null : recommendationAction.recommendationId();
	}
}
