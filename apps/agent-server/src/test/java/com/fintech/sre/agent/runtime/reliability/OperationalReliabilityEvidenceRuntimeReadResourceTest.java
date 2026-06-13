package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class OperationalReliabilityEvidenceRuntimeReadResourceTest {

	private final EvidenceRuntimeSummaryResource resource =
			new EvidenceRuntimeSummaryResource();

	@Test
	void shouldRemainReadOnly() {
		assertThat(resource.readOnly()).isTrue();
	}

	@Test
	void shouldNotTreatResponseAsRecommendationExecutionPermissionOrActionAdmission() {
		EvidenceRuntimeSummaryResponse response = resource.view(summary(
				EvidenceRuntimeSummaryStatus.HEALTHY,
				OperationalUncertainty.LOW,
				OperationalUncertainty.LOW,
				false,
				EvidenceRuntimeSummaryReason.UNKNOWN,
				true,
				EvidenceCompleteness.COMPLETE
		));

		assertThat(response.recommendation()).isFalse();
		assertThat(response.executionPermission()).isFalse();
		assertThat(response.actionAdmission()).isFalse();
	}

	@Test
	void shouldExposePaymentSafetyStateRiskLevelAndEvidenceCompleteness() {
		EvidenceRuntimeSummaryResponse response = resource.view(summary(
				EvidenceRuntimeSummaryStatus.UNCERTAIN,
				OperationalUncertainty.HIGH,
				OperationalUncertainty.HIGH,
				true,
				EvidenceRuntimeSummaryReason.PAYMENT_SAFETY_UNCERTAINTY,
				true,
				EvidenceCompleteness.PARTIAL
		));

		assertThat(response.summary().paymentSafetyState())
				.isEqualTo(OperationalUncertainty.HIGH);
		assertThat(response.summary().riskLevel())
				.isEqualTo(OperationalUncertainty.HIGH);
		assertThat(response.summary().evidenceCompleteness())
				.isEqualTo(EvidenceCompleteness.PARTIAL);
	}

	@Test
	void shouldExposeUncertaintyFlagAndReasonToOperators() {
		EvidenceRuntimeSummaryResponse response = resource.view(summary(
				EvidenceRuntimeSummaryStatus.UNCERTAIN,
				OperationalUncertainty.MODERATE,
				OperationalUncertainty.MODERATE,
				true,
				EvidenceRuntimeSummaryReason.UNKNOWN_EVIDENCE,
				false,
				EvidenceCompleteness.ABSENT
		));

		assertThat(response.summary().uncertaintyDetected()).isTrue();
		assertThat(response.summary().uncertaintyReason())
				.isEqualTo(EvidenceRuntimeSummaryReason.UNKNOWN_EVIDENCE);
		assertThat(response.resourceReason())
				.isEqualTo(EvidenceRuntimeSummaryResourceReason.UNKNOWN_EVIDENCE);
	}

	@Test
	void shouldExposeAdapterFailureAsEvidenceUncertaintyNotSystemFailure() {
		EvidenceRuntimeSummaryResponse response = resource.view(summary(
				EvidenceRuntimeSummaryStatus.UNCERTAIN,
				OperationalUncertainty.HIGH,
				OperationalUncertainty.MODERATE,
				true,
				EvidenceRuntimeSummaryReason.ADAPTER_FAILURE,
				false,
				EvidenceCompleteness.PARTIAL
		));

		assertThat(response.resourceStatus())
				.isEqualTo(EvidenceRuntimeSummaryResourceStatus.UNCERTAIN);
		assertThat(response.resourceReason())
				.isEqualTo(EvidenceRuntimeSummaryResourceReason.ADAPTER_FAILURE);
	}

	@Test
	void shouldExposePaymentInconsistencyAsCriticalOperatorFacingRisk() {
		EvidenceRuntimeSummaryResponse response = resource.view(summary(
				EvidenceRuntimeSummaryStatus.DEGRADED,
				OperationalUncertainty.CRITICAL,
				OperationalUncertainty.CRITICAL,
				true,
				EvidenceRuntimeSummaryReason.PAYMENT_INCONSISTENCY,
				true,
				EvidenceCompleteness.COMPLETE
		));

		assertThat(response.summary().riskLevel())
				.isEqualTo(OperationalUncertainty.CRITICAL);
		assertThat(response.resourceReason())
				.isEqualTo(EvidenceRuntimeSummaryResourceReason.PAYMENT_INCONSISTENCY);
	}

	@Test
	void shouldNotExposeRawPayloadVendorDetailOrCredentials() {
		EvidenceRuntimeSummaryResponse response = resource.view(summary(
				EvidenceRuntimeSummaryStatus.PARTIAL,
				OperationalUncertainty.MODERATE,
				OperationalUncertainty.LOW,
				true,
				EvidenceRuntimeSummaryReason.PARTIAL_EVIDENCE,
				true,
				EvidenceCompleteness.PARTIAL
		));

		assertThat(response.exposesRawPayload()).isFalse();
		assertThat(response.exposesVendorDetail()).isFalse();
		assertThat(response.exposesCredentialConfiguration()).isFalse();
		assertThat(response.summary().exposesRawPayload()).isFalse();
		assertThat(response.summary().exposesVendorDetail()).isFalse();
		assertThat(response.summary().exposesCredentialConfiguration()).isFalse();
	}

	@Test
	void shouldNotMutatePortfolioKnowledgeSource() {
		assertThat(resource.mutatesPortfolioKnowledgeSource()).isFalse();
	}

	@Test
	void shouldRejectNullSummary() {
		assertThatThrownBy(() -> resource.view(null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("summary must not be null");
	}

	private EvidenceRuntimeSummary summary(
			EvidenceRuntimeSummaryStatus status,
			OperationalUncertainty riskLevel,
			OperationalUncertainty paymentSafetyState,
			boolean uncertaintyDetected,
			EvidenceRuntimeSummaryReason reason,
			boolean auditTrusted,
			EvidenceCompleteness evidenceCompleteness
	) {
		return new EvidenceRuntimeSummary(
				status,
				riskLevel,
				paymentSafetyState,
				uncertaintyDetected,
				reason,
				auditTrusted,
				evidenceCompleteness
		);
	}
}
