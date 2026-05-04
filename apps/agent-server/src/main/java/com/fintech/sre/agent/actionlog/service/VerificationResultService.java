package com.fintech.sre.agent.actionlog.service;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.fintech.sre.agent.actionlog.dto.RecordVerificationRequest;
import com.fintech.sre.agent.actionlog.entity.ExecutedActionEntity;
import com.fintech.sre.agent.actionlog.entity.VerificationResultEntity;
import com.fintech.sre.agent.actionlog.repository.ExecutedActionRepository;
import com.fintech.sre.agent.actionlog.repository.VerificationResultRepository;
import com.fintech.sre.agent.exception.ActionLogNotFoundException;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class VerificationResultService {

	private final ExecutedActionRepository executedActionRepository;
	private final VerificationResultRepository verificationResultRepository;

	public Mono<VerificationResultEntity> record(
			String incidentId,
			Long actionId,
			RecordVerificationRequest request
	) {
		return Mono.fromSupplier(() -> {
			ExecutedActionEntity executedAction = executedActionRepository.findById(actionId)
					.orElseThrow(() -> new ActionLogNotFoundException("Executed action not found: " + actionId));
			if (!incidentId.equals(executedAction.incidentId())) {
				throw new ActionLogNotFoundException("Executed action does not belong to incident: " + incidentId);
			}

			return verificationResultRepository.save(VerificationResultEntity.builder()
					.incidentId(incidentId)
					.executedActionId(actionId)
					.metricName(request.metricName())
					.query(request.query())
					.beforeValue(request.beforeValue())
					.afterValue(request.afterValue())
					.expectedCondition(request.expectedCondition())
					.status(request.status())
					.checkedAt(request.checkedAt() == null ? Instant.now() : request.checkedAt())
					.build());
		});
	}
}
