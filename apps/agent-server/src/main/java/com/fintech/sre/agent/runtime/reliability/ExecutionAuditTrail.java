package com.fintech.sre.agent.runtime.reliability;

import java.util.List;
import java.util.Objects;

public record ExecutionAuditTrail(
		List<ExecutionAuditEvent> events
) {
	public ExecutionAuditTrail {
		Objects.requireNonNull(events, "events must not be null");
		events = List.copyOf(events);
	}

	public static ExecutionAuditTrail empty() {
		return new ExecutionAuditTrail(List.of());
	}

	public ExecutionAuditTrail append(ExecutionAuditEvent event) {
		Objects.requireNonNull(event, "event must not be null");

		List<ExecutionAuditEvent> appended = new java.util.ArrayList<>(events);
		appended.add(event);
		return new ExecutionAuditTrail(appended);
	}

	public boolean appendOnly() {
		return true;
	}

	public boolean overwritable() {
		return false;
	}

	public boolean hasHiddenDecision() {
		boolean hasAiOnlyDecision = events.stream()
				.anyMatch(event -> event.type()
						== ExecutionAuditEventType.AI_ONLY_DECISION_RECORDED);
		boolean hasPlanCreation = events.stream()
				.anyMatch(event -> event.type()
						== ExecutionAuditEventType.PLAN_CREATED);
		boolean hasEligibility = events.stream()
				.anyMatch(event -> event.type()
						== ExecutionAuditEventType.ELIGIBILITY_RECORDED);
		boolean hasApprovalOrRejection = events.stream()
				.anyMatch(event -> event.type()
						== ExecutionAuditEventType.APPROVAL_RECORDED
						|| event.type()
						== ExecutionAuditEventType.REJECTION_RECORDED);

		if (hasAiOnlyDecision && !hasApprovalOrRejection) {
			return true;
		}
		return hasPlanCreation && !hasEligibility;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}

	public ExecutionAuditDecision verify(boolean paymentImpactingPlan) {
		if (events.isEmpty()) {
			return new ExecutionAuditDecision(
					this,
					ExecutionAuditIntegrity.INCOMPLETE
			);
		}
		if (hasHiddenDecision()) {
			return new ExecutionAuditDecision(
					this,
					ExecutionAuditIntegrity.INCOMPLETE
			);
		}
		if (paymentImpactingPlan && events.stream().noneMatch(event -> event.type()
				== ExecutionAuditEventType.PLAN_CREATED)) {
			return new ExecutionAuditDecision(
					this,
					ExecutionAuditIntegrity.INCOMPLETE
			);
		}
		return new ExecutionAuditDecision(
				this,
				ExecutionAuditIntegrity.VERIFIED
		);
	}
}
