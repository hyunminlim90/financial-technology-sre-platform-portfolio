package com.fintech.sre.agent.actionlog.entity;

import java.time.Instant;

import lombok.Builder;

@Builder(toBuilder = true)
public record RollbackRecordEntity(
		Long id,
		String incidentId,
		Long executedActionId,
		String rollbackAction,
		String rollbackReason,
		String rollbackBy,
		Instant rollbackAt,
		String verificationStatus,
		Instant createdAt
) {
}
