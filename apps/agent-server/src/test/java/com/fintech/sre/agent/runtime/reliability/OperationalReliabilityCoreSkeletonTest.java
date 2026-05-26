package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class OperationalReliabilityCoreSkeletonTest {

	@Test
	void shouldPreserveCoreStateTransitionSemantics() {
		assertThat(RuntimeState.NORMAL.canTransitionTo(RuntimeState.DEGRADED)).isTrue();
		assertThat(RuntimeState.DEGRADED.canTransitionTo(RuntimeState.VERIFYING)).isTrue();
		assertThat(RuntimeState.VERIFYING.canTransitionTo(RuntimeState.ROLLING_BACK))
				.isTrue();
		assertThat(RuntimeState.ROLLING_BACK.canTransitionTo(RuntimeState.RECOVERING))
				.isTrue();
		assertThat(RuntimeState.RECOVERING.canTransitionTo(RuntimeState.NORMAL)).isTrue();
		assertThat(RuntimeState.NORMAL.canTransitionTo(RuntimeState.NORMAL)).isFalse();
		assertThat(RuntimeState.NORMAL.canTransitionTo(RuntimeState.ROLLING_BACK))
				.isFalse();
	}

	@Test
	void shouldRejectInvalidRuntimeStateTransition() {
		assertThatThrownBy(() -> new RuntimeStateTransition(
				RuntimeState.NORMAL,
				RuntimeState.ROLLING_BACK,
				"unsupported jump",
				Instant.parse("2026-05-25T00:00:00Z")
		))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("invalid runtime state transition");
	}

	@Test
	void shouldExposeRollbackTransitionPath() {
		RuntimeStateTransition transition = new RuntimeStateTransition(
				RuntimeState.VERIFYING,
				RuntimeState.ROLLING_BACK,
				"verification regression detected",
				Instant.parse("2026-05-25T00:00:00Z")
		);

		assertThat(transition.isRollbackPath()).isTrue();
	}

	@Test
	void shouldDefensivelyCopyEvidenceContext() {
		List<String> evidenceIds = new ArrayList<>(List.of("alert-1", "metric-1"));
		EvidenceContext evidenceContext = new EvidenceContext(evidenceIds, true, true);

		evidenceIds.add("log-1");

		assertThat(evidenceContext.evidenceIds())
				.containsExactly("alert-1", "metric-1");
		assertThatThrownBy(() -> evidenceContext.evidenceIds().add("mutated"))
				.isInstanceOf(UnsupportedOperationException.class);
		assertThat(evidenceContext.actionable()).isTrue();
	}

	@Test
	void shouldBoundReliabilityScore() {
		assertThat(new ReliabilityScore(85).degraded()).isFalse();
		assertThat(new ReliabilityScore(49).critical()).isTrue();
		assertThatThrownBy(() -> new ReliabilityScore(-1))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("reliability score must be between 0 and 100");
		assertThatThrownBy(() -> new ReliabilityScore(101))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("reliability score must be between 0 and 100");
	}

	@Test
	void shouldRequireHumanReviewWhenOperationalUncertaintyIsHigh() {
		ReliabilityAssessment assessment = new ReliabilityAssessment(
				RuntimeState.DEGRADED,
				new ReliabilityScore(61),
				VerificationResult.CONFIRMED,
				RollbackResult.NOT_ATTEMPTED,
				PropagationSignal.CROSS_SERVICE,
				OperationalUncertainty.HIGH,
				new EvidenceContext(List.of("alert-1"), true, true)
		);

		assertThat(assessment.degraded()).isTrue();
		assertThat(assessment.requiresHumanReview()).isTrue();
	}

	@Test
	void shouldRequireHumanReviewWhenVerificationIsInconclusive() {
		ReliabilityAssessment assessment = new ReliabilityAssessment(
				RuntimeState.VERIFYING,
				new ReliabilityScore(77),
				VerificationResult.INCONCLUSIVE,
				RollbackResult.PREPARED,
				PropagationSignal.LOCALIZED,
				OperationalUncertainty.MODERATE,
				new EvidenceContext(List.of("alert-2"), true, true)
		);

		assertThat(assessment.requiresHumanReview()).isTrue();
	}

	@Test
	void shouldRemainNonTerminalWhileVerificationIsPending() {
		assertThat(VerificationResult.PENDING.terminal()).isFalse();
		assertThat(VerificationResult.REGRESSION_DETECTED.terminal()).isTrue();
		assertThat(PropagationSignal.CROSS_CLUSTER.widespread()).isTrue();
		assertThat(OperationalUncertainty.CRITICAL.requiresHumanEscalation()).isTrue();
		assertThat(RollbackResult.FAILED.failed()).isTrue();
	}
}
