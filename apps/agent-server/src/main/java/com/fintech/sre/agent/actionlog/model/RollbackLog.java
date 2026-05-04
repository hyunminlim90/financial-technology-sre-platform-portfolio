package com.fintech.sre.agent.actionlog.model;

import java.time.Instant;

public record RollbackLog(
		Long executedActionId,
		String rollbackAction,
		String rollbackReason,
		String rollbackBy,
		Instant rollbackAt,
		String verificationStatus
) {
}
