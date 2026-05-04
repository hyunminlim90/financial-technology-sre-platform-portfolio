package com.fintech.sre.agent.actionlog.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fintech.sre.agent.actionlog.dto.RecordExecutedActionRequest;
import com.fintech.sre.agent.actionlog.dto.RecordRollbackRequest;
import com.fintech.sre.agent.actionlog.dto.RecordVerificationRequest;
import com.fintech.sre.agent.actionlog.entity.ExecutedActionEntity;
import com.fintech.sre.agent.actionlog.entity.RecommendationActionEntity;
import com.fintech.sre.agent.actionlog.entity.RollbackRecordEntity;
import com.fintech.sre.agent.actionlog.entity.VerificationResultEntity;
import com.fintech.sre.agent.actionlog.repository.ExecutedActionRepository;
import com.fintech.sre.agent.actionlog.repository.RecommendationActionRepository;
import com.fintech.sre.agent.actionlog.repository.RollbackRecordRepository;
import com.fintech.sre.agent.actionlog.repository.VerificationResultRepository;
import com.fintech.sre.agent.model.request.ExecutedAction;
import com.fintech.sre.agent.model.request.PostmortemGenerateRequest;
import com.fintech.sre.agent.model.request.RecommendationHistory;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ActionLogService {

	private final RecommendationLogService recommendationLogService;
	private final ExecutedActionService executedActionService;
	private final VerificationResultService verificationResultService;
	private final RollbackRecordService rollbackRecordService;
	private final ExecutedActionRepository executedActionRepository;
	private final RecommendationActionRepository recommendationActionRepository;
	private final VerificationResultRepository verificationResultRepository;
	private final RollbackRecordRepository rollbackRecordRepository;

	public Mono<ExecutedActionEntity> recordExecutedAction(String incidentId, RecordExecutedActionRequest request) {
		return executedActionService.record(incidentId, request);
	}

	public Mono<VerificationResultEntity> recordVerification(
			String incidentId,
			Long actionId,
			RecordVerificationRequest request
	) {
		return verificationResultService.record(incidentId, actionId, request);
	}

	public Mono<RollbackRecordEntity> recordRollback(
			String incidentId,
			Long actionId,
			RecordRollbackRequest request
	) {
		return rollbackRecordService.record(incidentId, actionId, request);
	}

	public Mono<PostmortemGenerateRequest> enrichPostmortemRequest(PostmortemGenerateRequest request) {
		Mono<List<RecommendationHistory>> recommendationHistoryMono = hasRecommendationHistory(request)
				? Mono.just(request.recommendationHistory())
				: recommendationLogService.findForPostmortem(request.incidentId());
		Mono<List<ExecutedAction>> executedActionsMono = hasExecutedActions(request)
				? Mono.just(request.executedActions())
				: findExecutedActionsForPostmortem(request.incidentId());

		return Mono.zip(recommendationHistoryMono, executedActionsMono)
				.map(tuple -> new PostmortemGenerateRequest(
						request.incidentId(),
						request.alertName(),
						request.service(),
						request.environment(),
						request.startTime(),
						request.endTime(),
						request.metricsSnapshot(),
						request.logsSample(),
						request.traceIds(),
						tuple.getT2(),
						tuple.getT1(),
						request.operatorSummary()
				));
	}

	private Mono<List<ExecutedAction>> findExecutedActionsForPostmortem(String incidentId) {
		return Mono.fromSupplier(() -> {
			Map<Long, RecommendationActionEntity> recommendationActions = recommendationActionRepository.findByIncidentId(incidentId)
					.stream()
					.filter(action -> action.id() != null)
					.collect(Collectors.toMap(RecommendationActionEntity::id, Function.identity()));
			Map<Long, List<VerificationResultEntity>> verifications = verificationResultRepository.findByIncidentId(incidentId)
					.stream()
					.collect(Collectors.groupingBy(VerificationResultEntity::executedActionId));
			Map<Long, List<RollbackRecordEntity>> rollbacks = rollbackRecordRepository.findByIncidentId(incidentId)
					.stream()
					.collect(Collectors.groupingBy(RollbackRecordEntity::executedActionId));

			return executedActionRepository.findByIncidentId(incidentId).stream()
					.map(entity -> mergeActionEvidence(entity, recommendationActions, verifications, rollbacks))
					.toList();
		});
	}

	private ExecutedAction mergeActionEvidence(
			ExecutedActionEntity action,
			Map<Long, RecommendationActionEntity> recommendationActions,
			Map<Long, List<VerificationResultEntity>> verifications,
			Map<Long, List<RollbackRecordEntity>> rollbacks
	) {
		List<String> verificationResults = verifications
				.getOrDefault(action.id(), List.of())
				.stream()
				.map(this::toVerificationResultText)
				.toList();
		boolean rollbackExecuted = action.rollbackExecuted() != null && action.rollbackExecuted();
		if (!rollbackExecuted && action.id() != null) {
			rollbackExecuted = !rollbacks.getOrDefault(action.id(), List.of()).isEmpty();
		}
		Integer step = action.recommendationActionId() == null ? null
				: recommendationActions.getOrDefault(action.recommendationActionId(), null) == null ? null
				: recommendationActions.get(action.recommendationActionId()).step();

		return new ExecutedAction(
				step,
				action.action(),
				action.executedAt(),
				action.executedBy(),
				action.expectedEffect(),
				action.actualEffect(),
				action.rollbackPlan(),
				rollbackExecuted,
				verificationResults
		);
	}

	private String toVerificationResultText(VerificationResultEntity verification) {
		String metric = verification.metricName() == null ? "metric" : verification.metricName();
		String condition = verification.expectedCondition() == null ? "condition not provided"
				: verification.expectedCondition();
		String before = verification.beforeValue() == null ? "n/a" : verification.beforeValue().toString();
		String after = verification.afterValue() == null ? "n/a" : verification.afterValue().toString();
		return "%s %s (%s -> %s)".formatted(
				verification.status(),
				metric,
				before,
				after
		) + " expected: " + condition;
	}

	private boolean hasRecommendationHistory(PostmortemGenerateRequest request) {
		return request.recommendationHistory() != null && !request.recommendationHistory().isEmpty();
	}

	private boolean hasExecutedActions(PostmortemGenerateRequest request) {
		return request.executedActions() != null && !request.executedActions().isEmpty();
	}
}
