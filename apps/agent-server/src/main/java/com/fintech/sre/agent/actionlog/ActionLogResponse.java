package com.fintech.sre.agent.actionlog;

import java.time.Instant;
import java.util.List;

import com.fintech.sre.agent.action.ActionCommand;

public record ActionLogResponse(
		String id,
		String incidentId,
		String scenarioId,
		String runbookId,
		String recommendedActionText,
		ActionCommand command,
		ActionLogStatus status,
		ActionOutcomeStatus outcomeStatus,
		String humanDecisionReason,
		String outcomeSummary,
		List<String> observedSignals,
		boolean postmortemRequired,
		Instant createdAt,
		Instant updatedAt
) {
	public static ActionLogResponse from(ActionLog log) {
		return new ActionLogResponse(
				log.id(),
				log.incidentId(),
				log.scenarioId(),
				log.runbookId(),
				log.recommendedActionText(),
				log.command(),
				log.status(),
				log.outcomeStatus(),
				log.humanDecisionReason(),
				log.outcomeSummary(),
				log.observedSignals(),
				log.postmortemRequired(),
				log.createdAt(),
				log.updatedAt()
		);
	}
}
