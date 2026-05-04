package com.fintech.sre.agent.actionlog;

import java.time.Instant;
import java.util.List;

import com.fintech.sre.agent.action.ActionCommand;

public record ActionLog(
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

	public ActionLog approve(String reason) {
		return new ActionLog(
				id,
				incidentId,
				scenarioId,
				runbookId,
				recommendedActionText,
				command,
				ActionLogStatus.APPROVED_BY_HUMAN,
				outcomeStatus,
				reason,
				outcomeSummary,
				observedSignals,
				postmortemRequired,
				createdAt,
				Instant.now()
		);
	}

	public ActionLog reject(String reason) {
		return new ActionLog(
				id,
				incidentId,
				scenarioId,
				runbookId,
				recommendedActionText,
				command,
				ActionLogStatus.REJECTED_BY_HUMAN,
				outcomeStatus,
				reason,
				outcomeSummary,
				observedSignals,
				postmortemRequired,
				createdAt,
				Instant.now()
		);
	}

	public ActionLog reportOutcome(
			ActionOutcomeStatus outcomeStatus,
			String outcomeSummary,
			List<String> observedSignals
	) {
		boolean needsPostmortem = outcomeStatus == ActionOutcomeStatus.PARTIALLY_MITIGATED
				|| outcomeStatus == ActionOutcomeStatus.NOT_EFFECTIVE
				|| outcomeStatus == ActionOutcomeStatus.CAUSED_SIDE_EFFECT
				|| outcomeStatus == ActionOutcomeStatus.ROLLED_BACK;

		return new ActionLog(
				id,
				incidentId,
				scenarioId,
				runbookId,
				recommendedActionText,
				command,
				needsPostmortem ? ActionLogStatus.POSTMORTEM_REQUIRED : ActionLogStatus.OUTCOME_REPORTED,
				outcomeStatus,
				humanDecisionReason,
				outcomeSummary,
				observedSignals,
				needsPostmortem,
				createdAt,
				Instant.now()
		);
	}
}
