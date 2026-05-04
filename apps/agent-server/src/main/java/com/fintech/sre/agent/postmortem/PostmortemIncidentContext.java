package com.fintech.sre.agent.postmortem;

import com.fintech.sre.agent.actionlog.model.IncidentActionLogSnapshot;
import com.fintech.sre.agent.model.common.IncidentContext;

public record PostmortemIncidentContext(
		IncidentContext incidentContext,
		IncidentActionLogSnapshot actionLogSnapshot,
		String operatorSummary
) {
}
