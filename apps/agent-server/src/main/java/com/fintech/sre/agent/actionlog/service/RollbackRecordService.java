package com.fintech.sre.agent.actionlog.service;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.fintech.sre.agent.actionlog.dto.RecordRollbackRequest;
import com.fintech.sre.agent.actionlog.entity.ExecutedActionEntity;
import com.fintech.sre.agent.actionlog.entity.RecommendationActionEntity;
import com.fintech.sre.agent.actionlog.entity.RollbackRecordEntity;
import com.fintech.sre.agent.actionlog.repository.ExecutedActionRepository;
import com.fintech.sre.agent.actionlog.repository.RecommendationActionRepository;
import com.fintech.sre.agent.actionlog.repository.RollbackRecordRepository;
import com.fintech.sre.agent.exception.ActionLogNotFoundException;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class RollbackRecordService {

	private final RollbackRecordRepository rollbackRecordRepository;
	private final ExecutedActionRepository executedActionRepository;
	private final RecommendationActionRepository recommendationActionRepository;

	public Mono<RollbackRecordEntity> record(
			String incidentId,
			Long actionId,
			RecordRollbackRequest request
	) {
		return Mono.fromSupplier(() -> {
			ExecutedActionEntity executedAction = executedActionRepository.findById(actionId)
					.orElseThrow(() -> new ActionLogNotFoundException("Executed action not found: " + actionId));
			if (!incidentId.equals(executedAction.incidentId())) {
				throw new ActionLogNotFoundException("Executed action does not belong to incident: " + incidentId);
			}
			executedActionRepository.save(executedAction.toBuilder()
					.rollbackExecuted(true)
					.build());
			updateRecommendationActionStatus(executedAction);

			return rollbackRecordRepository.save(RollbackRecordEntity.builder()
					.incidentId(incidentId)
					.executedActionId(actionId)
					.rollbackAction(request.rollbackAction())
					.rollbackReason(request.rollbackReason())
					.rollbackBy(request.rollbackBy())
					.rollbackAt(request.rollbackAt())
					.verificationStatus(request.verificationStatus())
					.createdAt(Instant.now())
					.build());
		});
	}

	private void updateRecommendationActionStatus(ExecutedActionEntity executedAction) {
		if (executedAction.recommendationActionId() == null) {
			return;
		}

		RecommendationActionEntity actionEntity = recommendationActionRepository
				.findById(executedAction.recommendationActionId())
				.orElseThrow(() -> new ActionLogNotFoundException(
						"Recommendation action not found: " + executedAction.recommendationActionId()
				));
		recommendationActionRepository.save(actionEntity.toBuilder()
				.status("ROLLED_BACK")
				.build());
	}
}
