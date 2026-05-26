package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

class OperationalReliabilityRegressionSkeletonTest {

	@Test
	void shouldTreatConvergedAsNonImmutableTruth() {
		RegressionDecision decision = RegressionDecision.evaluate(
				assessment(
						RuntimeState.CONVERGED,
						ConvergenceStatus.CONVERGED,
						List.of(signal(
								RegressionSignalType.SERVICE_DEGRADATION,
								"degradation-1"
						))
				)
		);

		assertThat(decision.regressionDetected()).isTrue();
		assertThat(decision.assessment().postConvergence()).isTrue();
	}

	@Test
	void shouldAllowDegradationSignalAfterConvergence() {
		RegressionDecision decision = RegressionDecision.evaluate(
				assessment(
						RuntimeState.CONVERGED,
						ConvergenceStatus.CONVERGED,
						List.of(signal(
								RegressionSignalType.SERVICE_DEGRADATION,
								"degradation-1"
						))
				)
		);

		assertThat(decision.regressionDetected()).isTrue();
		assertThat(decision.severity()).isEqualTo(RegressionSeverity.MODERATE);
	}

	@Test
	void shouldTreatPaymentSafetyRegressionAsHighSeverity() {
		RegressionDecision decision = RegressionDecision.evaluate(
				assessment(
						RuntimeState.CONVERGED,
						ConvergenceStatus.CONVERGED,
						List.of(signal(
								RegressionSignalType.PAYMENT_SAFETY,
								"payment-regression-1"
						))
				)
		);

		assertThat(decision.regressionDetected()).isTrue();
		assertThat(decision.severity()).isEqualTo(RegressionSeverity.HIGH);
	}

	@Test
	void shouldTreatPropagationReactivationAsConvergenceInvalidationCandidate() {
		RegressionDecision decision = RegressionDecision.evaluate(
				assessment(
						RuntimeState.CONVERGED,
						ConvergenceStatus.CONVERGED,
						List.of(signal(
								RegressionSignalType.PROPAGATION_REACTIVATED,
								"propagation-1"
						))
				)
		);

		assertThat(decision.regressionDetected()).isTrue();
		assertThat(decision.severity()).isEqualTo(RegressionSeverity.HIGH);
	}

	@Test
	void shouldTreatContradictoryPostConvergenceEvidenceAsRegressionCandidate() {
		RegressionDecision decision = RegressionDecision.evaluate(
				assessment(
						RuntimeState.CONVERGED,
						ConvergenceStatus.CONVERGED,
						List.of(signal(
								RegressionSignalType.CONTRADICTORY_EVIDENCE,
								"contradiction-1"
						))
				)
		);

		assertThat(decision.regressionDetected()).isTrue();
		assertThat(decision.severity()).isEqualTo(RegressionSeverity.HIGH);
	}

	@Test
	void shouldRemainSemanticDetectionOnly() {
		RegressionDecision decision = RegressionDecision.evaluate(
				assessment(
						RuntimeState.CONVERGED,
						ConvergenceStatus.CONVERGED,
						List.of(signal(
								RegressionSignalType.SERVICE_DEGRADATION,
								"degradation-1"
						))
				)
		);

		assertThat(decision.semanticDetectionOnly()).isTrue();
	}

	@Test
	void shouldRequireReverificationPathAfterRegression() {
		RegressionAssessment assessment = assessment(
				RuntimeState.CONVERGED,
				ConvergenceStatus.CONVERGED,
				List.of(signal(
						RegressionSignalType.SERVICE_DEGRADATION,
						"degradation-1"
				))
		);

		RegressionDecision decision = RegressionDecision.evaluate(assessment);

		assertThat(decision.regressionDetected()).isTrue();
		assertThat(assessment.requiresReverification()).isTrue();
	}

	@Test
	void shouldRejectNonPostConvergenceRegressionAssessment() {
		RegressionDecision decision = RegressionDecision.evaluate(
				assessment(
						RuntimeState.VERIFIED,
						ConvergenceStatus.CANDIDATE,
						List.of(signal(
								RegressionSignalType.SERVICE_DEGRADATION,
								"degradation-1"
						))
				)
		);

		assertThat(decision.regressionDetected()).isFalse();
		assertThat(decision.rejectionReason())
				.isEqualTo(RegressionRejectionReason.NOT_POST_CONVERGENCE_STATE);
	}

	@Test
	void shouldRejectWhenNoRegressionSignalsExist() {
		RegressionDecision decision = RegressionDecision.evaluate(
				assessment(
						RuntimeState.CONVERGED,
						ConvergenceStatus.CONVERGED,
						List.of()
				)
		);

		assertThat(decision.regressionDetected()).isFalse();
		assertThat(decision.rejectionReason())
				.isEqualTo(RegressionRejectionReason.NO_REGRESSION_SIGNALS);
	}

	@Test
	void shouldRejectNullAssessment() {
		assertThatThrownBy(() -> RegressionDecision.evaluate(null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("assessment must not be null");
	}

	private RegressionAssessment assessment(
			RuntimeState runtimeState,
			ConvergenceStatus convergenceStatus,
			List<RegressionSignal> signals
	) {
		return new RegressionAssessment(runtimeState, convergenceStatus, signals);
	}

	private RegressionSignal signal(
			RegressionSignalType type,
			String signalId
	) {
		return new RegressionSignal(type, signalId, "summary-" + signalId);
	}
}
