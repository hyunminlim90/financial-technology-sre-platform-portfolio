package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record ExecutionAuditDecision(
		ExecutionAuditTrail auditTrail,
		ExecutionAuditIntegrity integrity
) {
	public ExecutionAuditDecision {
		Objects.requireNonNull(auditTrail, "auditTrail must not be null");
		Objects.requireNonNull(integrity, "integrity must not be null");
	}

	public boolean executionPermission() {
		return false;
	}

	public boolean planTrustworthy() {
		return integrity == ExecutionAuditIntegrity.VERIFIED;
	}
}
