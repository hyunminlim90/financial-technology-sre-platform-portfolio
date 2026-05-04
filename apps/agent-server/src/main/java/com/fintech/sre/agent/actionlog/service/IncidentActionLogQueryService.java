package com.fintech.sre.agent.actionlog.service;

import org.springframework.stereotype.Service;

import com.fintech.sre.agent.actionlog.model.IncidentActionLogSnapshot;
import com.fintech.sre.agent.actionlog.repository.ExecutedActionRepository;
import com.fintech.sre.agent.actionlog.repository.RollbackRecordRepository;
import com.fintech.sre.agent.actionlog.repository.VerificationResultRepository;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class IncidentActionLogQueryService {

	private final RecommendationLogService recommendationLogService;
	private final ExecutedActionRepository executedActionRepository;
	private final VerificationResultRepository verificationResultRepository;
	private final RollbackRecordRepository rollbackRecordRepository;

	public Mono<IncidentActionLogSnapshot> findSnapshot(String incidentId) {
		return Mono.fromSupplier(() -> IncidentActionLogSnapshotMapper.map(
				incidentId,
				recommendationLogService.findLogs(incidentId),
				executedActionRepository.findByIncidentId(incidentId),
				verificationResultRepository.findByIncidentId(incidentId),
				rollbackRecordRepository.findByIncidentId(incidentId)
		));
	}
}
