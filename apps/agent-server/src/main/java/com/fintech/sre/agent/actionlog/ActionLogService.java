package com.fintech.sre.agent.actionlog;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fintech.sre.agent.model.common.RecommendedAction;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service("learningActionLogService")
public class ActionLogService {

	private final ActionLogRepository repository;

	public ActionLogService(ActionLogRepository repository) {
		this.repository = repository;
	}

	public Mono<ActionLog> recordRecommendation(
			String incidentId,
			String scenarioId,
			String runbookId,
			RecommendedAction action
	) {
		return recordRecommendation(incidentId, scenarioId, runbookId, action, List.of());
	}

	public Mono<ActionLog> recordRecommendation(
			String incidentId,
			String scenarioId,
			String runbookId,
			RecommendedAction action,
			List<String> observedSignals
	) {
		Instant now = Instant.now();

		ActionLog log = new ActionLog(
				UUID.randomUUID().toString(),
				incidentId,
				scenarioId,
				runbookId,
				action.action(),
				action.command(),
				ActionLogStatus.RECOMMENDED,
				ActionOutcomeStatus.NOT_REPORTED,
				null,
				null,
				observedSignals == null ? List.of() : observedSignals,
				false,
				now,
				now
		);

		return repository.save(log);
	}

	public Mono<ActionLog> approve(String actionLogId, String reason) {
		return repository.findById(actionLogId)
				.switchIfEmpty(Mono.error(new IllegalArgumentException("ActionLog not found: " + actionLogId)))
				.flatMap(log -> repository.save(log.approve(reason)));
	}

	public Mono<ActionLog> reject(String actionLogId, String reason) {
		return repository.findById(actionLogId)
				.switchIfEmpty(Mono.error(new IllegalArgumentException("ActionLog not found: " + actionLogId)))
				.flatMap(log -> repository.save(log.reject(reason)));
	}

	public Mono<ActionLog> reportOutcome(String actionLogId, ActionOutcomeRequest request) {
		return repository.findById(actionLogId)
				.switchIfEmpty(Mono.error(new IllegalArgumentException("ActionLog not found: " + actionLogId)))
				.flatMap(log -> repository.save(log.reportOutcome(
						request.outcomeStatus(),
						request.outcomeSummary(),
						request.observedSignals() == null ? List.of() : request.observedSignals()
				)));
	}

	public Flux<ActionLog> findByIncidentId(String incidentId) {
		return repository.findByIncidentId(incidentId);
	}

	public Flux<ActionLog> findPostmortemRequired() {
		return repository.findPostmortemRequired();
	}
}
