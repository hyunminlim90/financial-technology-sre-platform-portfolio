package com.fintech.sre.agent.runtime.reliability;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

public record LifecycleAuditTrail(
		List<LifecycleAuditEvent> events
) {
	private static final EnumSet<LifecycleAuditEventType> REQUIRED_STAGE_EVENTS =
			EnumSet.of(
					LifecycleAuditEventType.ASSESSMENT_RECORDED,
					LifecycleAuditEventType.ADMISSION_RECORDED,
					LifecycleAuditEventType.READINESS_RECORDED,
					LifecycleAuditEventType.EXECUTOR_RESPONSE_RECORDED,
					LifecycleAuditEventType.POST_VERIFICATION_RECORDED,
					LifecycleAuditEventType.POST_CONVERGENCE_RECORDED,
					LifecycleAuditEventType.POST_REGRESSION_RECORDED
			);

	public LifecycleAuditTrail {
		Objects.requireNonNull(events, "events must not be null");
		events = List.copyOf(events);
	}

	public static LifecycleAuditTrail empty() {
		return new LifecycleAuditTrail(List.of());
	}

	public LifecycleAuditTrail append(LifecycleAuditEvent event) {
		Objects.requireNonNull(event, "event must not be null");

		List<LifecycleAuditEvent> appended = new java.util.ArrayList<>(events);
		appended.add(event);
		return new LifecycleAuditTrail(appended);
	}

	public boolean appendOnly() {
		return true;
	}

	public boolean overwritable() {
		return false;
	}

	public boolean hasHiddenDecision() {
		EnumSet<LifecycleAuditEventType> seen = EnumSet.noneOf(
				LifecycleAuditEventType.class
		);
		events.forEach(event -> seen.add(event.type()));

		boolean hasAiOnlyDecision = seen.contains(
				LifecycleAuditEventType.AI_ONLY_DECISION_RECORDED
		);
		boolean missingStageCoverage = !seen.containsAll(REQUIRED_STAGE_EVENTS);

		if (hasAiOnlyDecision && !seen.contains(
				LifecycleAuditEventType.ADMISSION_RECORDED
		)) {
			return true;
		}
		return missingStageCoverage;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}

	public LifecycleAuditDecision verify(boolean paymentImpactingLifecycle) {
		if (events.isEmpty()) {
			return new LifecycleAuditDecision(
					this,
					LifecycleAuditIntegrity.INCOMPLETE
			);
		}
		if (hasHiddenDecision()) {
			return new LifecycleAuditDecision(
					this,
					LifecycleAuditIntegrity.INCOMPLETE
			);
		}
		if (paymentImpactingLifecycle && events.isEmpty()) {
			return new LifecycleAuditDecision(
					this,
					LifecycleAuditIntegrity.INCOMPLETE
			);
		}
		return new LifecycleAuditDecision(
				this,
				LifecycleAuditIntegrity.VERIFIED
		);
	}
}
