package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class OperationalReliabilityLifecycleSummaryResourceSkeletonTest {

	private final ReliabilityLifecycleSummaryResource resource =
			new ReliabilityLifecycleSummaryResource();

	@Test
	void shouldRemainReadOnly() {
		assertThat(resource.readOnly()).isTrue();
	}

	@Test
	void shouldNotTreatResponseAsRecommendationOrExecutionPermission() {
		ReliabilityLifecycleSummaryResponse response = resource.view(summary(
				ReliabilityLifecycleSummaryStatus.STABLE,
				true,
				OperationalUncertainty.LOW,
				RuntimeState.CONVERGED,
				ReliabilityLifecycleSummaryReason.STABLE_CONVERGENCE_CONFIRMED
		));

		assertThat(response.recommendation()).isFalse();
		assertThat(response.executionPermission()).isFalse();
	}

	@Test
	void shouldExposeAuditTrustedFlagToOperators() {
		ReliabilityLifecycleSummaryResponse response = resource.view(summary(
				ReliabilityLifecycleSummaryStatus.RECOVERED,
				false,
				OperationalUncertainty.MODERATE,
				RuntimeState.VERIFIED,
				ReliabilityLifecycleSummaryReason.AUDIT_INTEGRITY_INCOMPLETE
		));

		assertThat(response.summary().auditTrusted()).isFalse();
	}

	@Test
	void shouldExposePaymentRiskToOperators() {
		ReliabilityLifecycleSummaryResponse response = resource.view(summary(
				ReliabilityLifecycleSummaryStatus.UNCERTAIN,
				true,
				OperationalUncertainty.CRITICAL,
				RuntimeState.DEGRADED,
				ReliabilityLifecycleSummaryReason.PAYMENT_INCONSISTENCY_DETECTED
		));

		assertThat(response.summary().risk()).isEqualTo(OperationalUncertainty.CRITICAL);
	}

	@Test
	void shouldExposeRegressionDetectedToOperators() {
		ReliabilityLifecycleSummaryResponse response = resource.view(summary(
				ReliabilityLifecycleSummaryStatus.UNCERTAIN,
				true,
				OperationalUncertainty.HIGH,
				RuntimeState.DEGRADED,
				ReliabilityLifecycleSummaryReason.REGRESSION_DETECTED
		));

		assertThat(response.summary().regressionDetected()).isTrue();
	}

	@Test
	void shouldExposeUncertaintyReasonToOperators() {
		ReliabilityLifecycleSummaryResponse response = resource.view(summary(
				ReliabilityLifecycleSummaryStatus.UNCERTAIN,
				true,
				OperationalUncertainty.HIGH,
				RuntimeState.UNSTABLE,
				ReliabilityLifecycleSummaryReason.EXECUTOR_RESPONSE_UNKNOWN
		));

		assertThat(response.summary().uncertaintyReason()).isEqualTo(
				ReliabilityLifecycleSummaryReason.EXECUTOR_RESPONSE_UNKNOWN
		);
	}

	@Test
	void shouldNotExposeInternalRawEvidencePayload() {
		ReliabilityLifecycleSummaryResponse response = resource.view(summary(
				ReliabilityLifecycleSummaryStatus.RECOVERED,
				true,
				OperationalUncertainty.MODERATE,
				RuntimeState.VERIFIED,
				ReliabilityLifecycleSummaryReason.POST_EXECUTION_VERIFICATION_CONFIRMED
		));

		assertThat(response.exposesRawEvidencePayload()).isFalse();
	}

	@Test
	void shouldNotMutatePortfolioKnowledgeSource() {
		assertThat(resource.mutatesPortfolioKnowledgeSource()).isFalse();
	}

	@Test
	void shouldMapSummaryStatusToViewStatus() {
		ReliabilityLifecycleSummaryResponse response = resource.view(summary(
				ReliabilityLifecycleSummaryStatus.FAILED,
				true,
				OperationalUncertainty.HIGH,
				RuntimeState.FAILED,
				ReliabilityLifecycleSummaryReason.EXECUTOR_RESPONSE_FAILED
		));

		assertThat(response.summary().status()).isEqualTo(
				ReliabilityLifecycleSummaryViewStatus.FAILED
		);
	}

	@Test
	void shouldRejectNullSummary() {
		assertThatThrownBy(() -> resource.view(null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("summary must not be null");
	}

	private ReliabilityLifecycleSummary summary(
			ReliabilityLifecycleSummaryStatus status,
			boolean trusted,
			OperationalUncertainty risk,
			RuntimeState lifecycleState,
			ReliabilityLifecycleSummaryReason reason
	) {
		return new ReliabilityLifecycleSummary(
				status,
				ReliabilityLifecycleSummaryScope.OPERATOR_READ_MODEL,
				trusted,
				risk,
				lifecycleState,
				reason
		);
	}
}
