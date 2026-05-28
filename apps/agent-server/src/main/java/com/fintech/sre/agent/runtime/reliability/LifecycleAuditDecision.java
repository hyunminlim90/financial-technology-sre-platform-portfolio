package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record LifecycleAuditDecision(
		LifecycleAuditTrail auditTrail,
		LifecycleAuditIntegrity integrity
) {
	public LifecycleAuditDecision {
		Objects.requireNonNull(auditTrail, "auditTrail must not be null");
		Objects.requireNonNull(integrity, "integrity must not be null");
	}

	public boolean recommendation() {
		return false;
	}

	public boolean executionPermission() {
		return false;
	}

	public boolean lifecycleTrustworthy() {
		return integrity == LifecycleAuditIntegrity.VERIFIED;
	}
}
