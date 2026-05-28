package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class OperationalReliabilityExecutionAuditSkeletonTest {

	@Test
	void shouldTreatAuditTrailAsAppendOnly() {
		ExecutionAuditTrail empty = ExecutionAuditTrail.empty();
		ExecutionAuditTrail appended = empty.append(event(
				ExecutionAuditEventType.ELIGIBILITY_RECORDED,
				"eligibility-1"
		));

		assertThat(empty.events()).isEmpty();
		assertThat(appended.events()).hasSize(1);
		assertThat(appended.appendOnly()).isTrue();
	}

	@Test
	void shouldAuditRejectionApprovalEligibilityAndPlanCreation() {
		ExecutionAuditTrail trail = ExecutionAuditTrail.empty()
				.append(event(
						ExecutionAuditEventType.REJECTION_RECORDED,
						"rejection-1"
				))
				.append(event(
						ExecutionAuditEventType.APPROVAL_RECORDED,
						"approval-1"
				))
				.append(event(
						ExecutionAuditEventType.ELIGIBILITY_RECORDED,
						"eligibility-1"
				))
				.append(event(
						ExecutionAuditEventType.PLAN_CREATED,
						"plan-1"
				));

		assertThat(trail.events()).extracting(ExecutionAuditEvent::type)
				.containsExactly(
						ExecutionAuditEventType.REJECTION_RECORDED,
						ExecutionAuditEventType.APPROVAL_RECORDED,
						ExecutionAuditEventType.ELIGIBILITY_RECORDED,
						ExecutionAuditEventType.PLAN_CREATED
				);
	}

	@Test
	void shouldNotTreatAuditAsExecutionPermission() {
		ExecutionAuditDecision decision = ExecutionAuditTrail.empty()
				.append(event(
						ExecutionAuditEventType.ELIGIBILITY_RECORDED,
						"eligibility-1"
				))
				.verify(false);

		assertThat(decision.executionPermission()).isFalse();
	}

	@Test
	void shouldDisallowAuditOverwrite() {
		assertThat(ExecutionAuditTrail.empty().overwritable()).isFalse();
	}

	@Test
	void shouldDetectHiddenDecisionWhenPlanExistsWithoutEligibility() {
		ExecutionAuditTrail trail = ExecutionAuditTrail.empty()
				.append(event(
						ExecutionAuditEventType.PLAN_CREATED,
						"plan-1"
				));

		assertThat(trail.hasHiddenDecision()).isTrue();
		assertThat(trail.verify(false).integrity())
				.isEqualTo(ExecutionAuditIntegrity.INCOMPLETE);
	}

	@Test
	void shouldRecordAiOnlyDecisionExplicitlyInAudit() {
		ExecutionAuditTrail trail = ExecutionAuditTrail.empty()
				.append(event(
						ExecutionAuditEventType.AI_ONLY_DECISION_RECORDED,
						"ai-only-1"
				))
				.append(event(
						ExecutionAuditEventType.REJECTION_RECORDED,
						"rejection-1"
				));

		assertThat(trail.events()).extracting(ExecutionAuditEvent::type)
				.contains(ExecutionAuditEventType.AI_ONLY_DECISION_RECORDED);
		assertThat(trail.hasHiddenDecision()).isFalse();
	}

	@Test
	void shouldRequireAuditForPaymentImpactingPlanTrust() {
		ExecutionAuditTrail trail = ExecutionAuditTrail.empty();

		ExecutionAuditDecision decision = trail.verify(true);

		assertThat(decision.integrity()).isEqualTo(ExecutionAuditIntegrity.INCOMPLETE);
		assertThat(decision.planTrustworthy()).isFalse();
	}

	@Test
	void shouldTrustExecutionPlanOnlyWhenAuditIntegrityExists() {
		ExecutionAuditTrail trail = ExecutionAuditTrail.empty()
				.append(event(
						ExecutionAuditEventType.APPROVAL_RECORDED,
						"approval-1"
				))
				.append(event(
						ExecutionAuditEventType.ELIGIBILITY_RECORDED,
						"eligibility-1"
				))
				.append(event(
						ExecutionAuditEventType.PLAN_CREATED,
						"plan-1"
				));

		ExecutionAuditDecision decision = trail.verify(true);

		assertThat(decision.integrity()).isEqualTo(ExecutionAuditIntegrity.VERIFIED);
		assertThat(decision.planTrustworthy()).isTrue();
	}

	@Test
	void shouldNotMutatePortfolioKnowledgeSource() {
		assertThat(ExecutionAuditTrail.empty().mutatesPortfolioKnowledgeSource()).isFalse();
	}

	@Test
	void shouldRejectNullEventAppend() {
		assertThatThrownBy(() -> ExecutionAuditTrail.empty().append(null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("event must not be null");
	}

	private ExecutionAuditEvent event(
			ExecutionAuditEventType type,
			String id
	) {
		return new ExecutionAuditEvent(
				type,
				id,
				"summary-" + id,
				Instant.parse("2026-05-28T00:00:00Z")
		);
	}
}
