package com.fintech.sre.agent.actionlog.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RecordRollbackRequest(
		@NotBlank String rollbackAction,
		String rollbackReason,
		@NotBlank String rollbackBy,
		@NotNull Instant rollbackAt,
		String verificationStatus
) {
}
