package com.fintech.sre.agent.postmortem;

import java.util.List;

import com.fintech.sre.agent.actionlog.ActionLog;

public record PostmortemDraftInput(
		String incidentId,
		List<ActionLog> actionLogs
) {
}
