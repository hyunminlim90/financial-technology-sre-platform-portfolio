package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class OperationalReliabilityLifecycleAuditSkeletonTest {

	@Test
	void shouldTreatLifecycleAuditTrailAsAppendOnly() {
		LifecycleAuditTrail empty = LifecycleAuditTrail.empty();
		LifecycleAuditTrail appended = empty.append(event(
				LifecycleAuditEventType.ASSESSMENT_RECORDED,
				"assessment-1"
		));

		assertThat(empty.events()).isEmpty();
		assertThat(appended.events()).hasSize(1);
		assertThat(appended.appendOnly()).isTrue();
	}

	@Test
	void shouldAuditEntireLifecycleStages() {
		LifecycleAuditTrail trail = completeLifecycleTrail();

		assertThat(trail.events()).extracting(LifecycleAuditEvent::type)
				.containsExactly(
						LifecycleAuditEventType.ASSESSMENT_RECORDED,
						LifecycleAuditEventType.ADMISSION_RECORDED,
						LifecycleAuditEventType.READINESS_RECORDED,
						LifecycleAuditEventType.EXECUTOR_RESPONSE_RECORDED,
						LifecycleAuditEventType.POST_VERIFICATION_RECORDED,
						LifecycleAuditEventType.POST_CONVERGENCE_RECORDED,
						LifecycleAuditEventType.POST_REGRESSION_RECORDED
				);
	}

	@Test
	void shouldNotTrustStableLifecycleWithoutAuditIntegrity() {
		LifecycleAuditDecision decision = LifecycleAuditTrail.empty()
				.append(event(
						LifecycleAuditEventType.ASSESSMENT_RECORDED,
						"assessment-1"
				))
				.verify(false);

		assertThat(decision.integrity()).isEqualTo(LifecycleAuditIntegrity.INCOMPLETE);
		assertThat(decision.lifecycleTrustworthy()).isFalse();
	}

	@Test
	void shouldRequireAuditForPaymentImpactingLifecycle() {
		LifecycleAuditDecision decision = LifecycleAuditTrail.empty().verify(true);

		assertThat(decision.integrity()).isEqualTo(LifecycleAuditIntegrity.INCOMPLETE);
		assertThat(decision.lifecycleTrustworthy()).isFalse();
	}

	@Test
	void shouldDetectHiddenLifecycleDecisionWhenStageCoverageMissing() {
		LifecycleAuditTrail trail = LifecycleAuditTrail.empty()
				.append(event(
						LifecycleAuditEventType.EXECUTOR_RESPONSE_RECORDED,
						"executor-1"
				));

		assertThat(trail.hasHiddenDecision()).isTrue();
		assertThat(trail.verify(false).integrity())
				.isEqualTo(LifecycleAuditIntegrity.INCOMPLETE);
	}

	@Test
	void shouldRecordAiOnlyLifecycleDecisionExplicitly() {
		LifecycleAuditTrail trail = completeLifecycleTrail()
				.append(event(
						LifecycleAuditEventType.AI_ONLY_DECISION_RECORDED,
						"ai-only-1"
				));

		assertThat(trail.events()).extracting(LifecycleAuditEvent::type)
				.contains(LifecycleAuditEventType.AI_ONLY_DECISION_RECORDED);
		assertThat(trail.hasHiddenDecision()).isFalse();
	}

	@Test
	void shouldNotTreatLifecycleAuditAsRecommendationOrExecutionPermission() {
		LifecycleAuditDecision decision = completeLifecycleTrail().verify(false);

		assertThat(decision.recommendation()).isFalse();
		assertThat(decision.executionPermission()).isFalse();
	}

	@Test
	void shouldVerifyLifecycleOnlyWhenIntegrityExists() {
		LifecycleAuditDecision decision = completeLifecycleTrail().verify(true);

		assertThat(decision.integrity()).isEqualTo(LifecycleAuditIntegrity.VERIFIED);
		assertThat(decision.lifecycleTrustworthy()).isTrue();
	}

	@Test
	void shouldNotMutatePortfolioKnowledgeSource() {
		assertThat(LifecycleAuditTrail.empty().mutatesPortfolioKnowledgeSource())
				.isFalse();
	}

	@Test
	void shouldRejectNullEventAppend() {
		assertThatThrownBy(() -> LifecycleAuditTrail.empty().append(null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("event must not be null");
	}

	private LifecycleAuditTrail completeLifecycleTrail() {
		return LifecycleAuditTrail.empty()
				.append(event(LifecycleAuditEventType.ASSESSMENT_RECORDED, "assessment-1"))
				.append(event(LifecycleAuditEventType.ADMISSION_RECORDED, "admission-1"))
				.append(event(LifecycleAuditEventType.READINESS_RECORDED, "readiness-1"))
				.append(event(
						LifecycleAuditEventType.EXECUTOR_RESPONSE_RECORDED,
						"executor-1"
				))
				.append(event(
						LifecycleAuditEventType.POST_VERIFICATION_RECORDED,
						"verification-1"
				))
				.append(event(
						LifecycleAuditEventType.POST_CONVERGENCE_RECORDED,
						"convergence-1"
				))
				.append(event(
						LifecycleAuditEventType.POST_REGRESSION_RECORDED,
						"regression-1"
				));
	}

	private LifecycleAuditEvent event(
			LifecycleAuditEventType type,
			String id
	) {
		return new LifecycleAuditEvent(
				type,
				id,
				"summary-" + id,
				Instant.parse("2026-05-28T00:00:00Z")
		);
	}
}
