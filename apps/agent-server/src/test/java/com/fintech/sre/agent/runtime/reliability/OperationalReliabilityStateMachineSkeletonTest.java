package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class OperationalReliabilityStateMachineSkeletonTest {

	private final RuntimeTransitionGuard guard = new RuntimeTransitionGuard();
	private final RuntimeStateMachine stateMachine = new RuntimeStateMachine(guard);

	@Test
	void shouldRejectUnknownToConverged() {
		RuntimeTransitionDecision decision = stateMachine.transition(
				RuntimeState.UNKNOWN,
				RuntimeState.CONVERGED,
				assessment(
						RuntimeState.UNKNOWN,
						VerificationResult.PENDING,
						RollbackResult.PREPARED,
						OperationalUncertainty.MODERATE
				),
				"unknown directly converged",
				Instant.parse("2026-05-25T00:00:00Z")
		);

		assertThat(decision.allowed()).isFalse();
		assertThat(decision.rejectionReason())
				.isEqualTo(
						RuntimeTransitionRejectionReason.UNKNOWN_CANNOT_CONVERGE_DIRECTLY
				);
	}

	@Test
	void shouldRejectPropagatingToConvergedWithoutVerification() {
		RuntimeTransitionDecision decision = stateMachine.transition(
				RuntimeState.PROPAGATING,
				RuntimeState.CONVERGED,
				assessment(
						RuntimeState.PROPAGATING,
						VerificationResult.INCONCLUSIVE,
						RollbackResult.PREPARED,
						OperationalUncertainty.MODERATE
				),
				"propagation not verified",
				Instant.parse("2026-05-25T00:00:00Z")
		);

		assertThat(decision.allowed()).isFalse();
		assertThat(decision.rejectionReason())
				.isEqualTo(
						RuntimeTransitionRejectionReason.PROPAGATING_REQUIRES_VERIFICATION
				);
	}

	@Test
	void shouldRejectRollbackingToRecoveringUntilVerifiedOrFailed() {
		RuntimeTransitionDecision decision = stateMachine.transition(
				RuntimeState.ROLLBACKING,
				RuntimeState.RECOVERING,
				assessment(
						RuntimeState.ROLLBACKING,
						VerificationResult.PENDING,
						RollbackResult.APPLIED,
						OperationalUncertainty.MODERATE
				),
				"rollback path not yet verified",
				Instant.parse("2026-05-25T00:00:00Z")
		);

		assertThat(decision.allowed()).isFalse();
		assertThat(decision.rejectionReason())
				.isEqualTo(
						RuntimeTransitionRejectionReason.ROLLBACK_REQUIRES_VERIFIED_OR_FAILED
				);
	}

	@Test
	void shouldAllowConvergedOnlyAfterVerified() {
		RuntimeTransitionDecision decision = stateMachine.transition(
				RuntimeState.VERIFIED,
				RuntimeState.CONVERGED,
				assessment(
						RuntimeState.VERIFIED,
						VerificationResult.CONFIRMED,
						RollbackResult.PREPARED,
						OperationalUncertainty.MODERATE
				),
				"verified convergence",
				Instant.parse("2026-05-25T00:00:00Z")
		);

		assertThat(decision.allowed()).isTrue();
		assertThat(decision.transition()).isNotNull();
		assertThat(decision.transition().from()).isEqualTo(RuntimeState.VERIFIED);
		assertThat(decision.transition().to()).isEqualTo(RuntimeState.CONVERGED);
	}

	@Test
	void shouldRejectConvergedBeforeVerified() {
		RuntimeTransitionDecision decision = stateMachine.transition(
				RuntimeState.RECOVERING,
				RuntimeState.CONVERGED,
				assessment(
						RuntimeState.RECOVERING,
						VerificationResult.CONFIRMED,
						RollbackResult.PREPARED,
						OperationalUncertainty.MODERATE
				),
				"premature convergence",
				Instant.parse("2026-05-25T00:00:00Z")
		);

		assertThat(decision.allowed()).isFalse();
		assertThat(decision.rejectionReason())
				.isEqualTo(
						RuntimeTransitionRejectionReason.CONVERGENCE_REQUIRES_VERIFIED_STATE
				);
	}

	@Test
	void shouldTreatFailedAsTerminal() {
		RuntimeTransitionDecision decision = stateMachine.transition(
				RuntimeState.FAILED,
				RuntimeState.RECOVERING,
				assessment(
						RuntimeState.FAILED,
						VerificationResult.REGRESSION_DETECTED,
						RollbackResult.FAILED,
						OperationalUncertainty.CRITICAL
				),
				"failed should not recover directly",
				Instant.parse("2026-05-25T00:00:00Z")
		);

		assertThat(decision.allowed()).isFalse();
		assertThat(decision.rejectionReason())
				.isEqualTo(RuntimeTransitionRejectionReason.FAILED_IS_TERMINAL);
	}

	@Test
	void shouldRejectConvergedWhenPaymentSafetyUncertaintyExists() {
		RuntimeTransitionDecision decision = stateMachine.transition(
				RuntimeState.VERIFIED,
				RuntimeState.CONVERGED,
				assessment(
						RuntimeState.VERIFIED,
						VerificationResult.CONFIRMED,
						RollbackResult.PREPARED,
						OperationalUncertainty.CRITICAL
				),
				"payment safety uncertainty",
				Instant.parse("2026-05-25T00:00:00Z")
		);

		assertThat(decision.allowed()).isFalse();
		assertThat(decision.rejectionReason())
				.isEqualTo(
						RuntimeTransitionRejectionReason.PAYMENT_SAFETY_UNCERTAINTY
				);
	}

	@Test
	void shouldRejectRollbackingEntryWhenRollbackUnavailable() {
		RuntimeTransitionDecision decision = stateMachine.transition(
				RuntimeState.PROPAGATING,
				RuntimeState.ROLLBACKING,
				assessment(
						RuntimeState.PROPAGATING,
						VerificationResult.CONFIRMED,
						RollbackResult.NOT_ATTEMPTED,
						OperationalUncertainty.MODERATE
				),
				"rollback unavailable",
				Instant.parse("2026-05-25T00:00:00Z")
		);

		assertThat(decision.allowed()).isFalse();
		assertThat(decision.rejectionReason())
				.isEqualTo(RuntimeTransitionRejectionReason.ROLLBACK_UNAVAILABLE);
	}

	@Test
	void shouldRejectNullTransitionGuard() {
		assertThatThrownBy(() -> new RuntimeStateMachine(null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("transitionGuard must not be null");
	}

	private ReliabilityAssessment assessment(
			RuntimeState runtimeState,
			VerificationResult verificationResult,
			RollbackResult rollbackResult,
			OperationalUncertainty operationalUncertainty
	) {
		return new ReliabilityAssessment(
				runtimeState,
				new ReliabilityScore(72),
				verificationResult,
				rollbackResult,
				PropagationSignal.CROSS_SERVICE,
				operationalUncertainty,
				new EvidenceContext(List.of("alert-1"), true, true)
		);
	}
}
