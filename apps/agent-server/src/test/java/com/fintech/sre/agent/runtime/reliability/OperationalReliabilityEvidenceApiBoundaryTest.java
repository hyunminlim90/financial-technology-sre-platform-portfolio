package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class OperationalReliabilityEvidenceApiBoundaryTest {

	private final EvidenceRuntimeApiBoundary boundary =
			new EvidenceRuntimeApiBoundary();

	@Test
	void shouldRemainReadOnlyAndNotBeActualHttpEndpoint() {
		assertThat(boundary.readOnly()).isTrue();
		assertThat(boundary.actualHttpEndpoint()).isFalse();
	}

	@Test
	void shouldKeepApiResponseAsNonRecommendationNonExecutionAndNonAdmission() {
		EvidenceRuntimeApiResponse response = boundary.read(request(
				new EvidenceRuntimeSummaryResource(),
				summary(
						EvidenceRuntimeSummaryStatus.HEALTHY,
						OperationalUncertainty.LOW,
						OperationalUncertainty.LOW,
						false,
						EvidenceRuntimeSummaryReason.UNKNOWN,
						true,
						EvidenceCompleteness.COMPLETE
				)
		));

		assertThat(response.readOnly()).isTrue();
		assertThat(response.recommendation()).isFalse();
		assertThat(response.executionPermission()).isFalse();
		assertThat(response.actionAdmission()).isFalse();
	}

	@Test
	void shouldExposePaymentSafetyStateRiskUncertaintyEvidenceCompletenessAndAuditTrust() {
		EvidenceRuntimeApiResponse response = boundary.read(request(
				new EvidenceRuntimeSummaryResource(),
				summary(
						EvidenceRuntimeSummaryStatus.UNCERTAIN,
						OperationalUncertainty.HIGH,
						OperationalUncertainty.HIGH,
						true,
						EvidenceRuntimeSummaryReason.PAYMENT_SAFETY_UNCERTAINTY,
						true,
						EvidenceCompleteness.PARTIAL
				)
		));

		assertThat(response.summary().paymentSafetyState()).isEqualTo(OperationalUncertainty.HIGH);
		assertThat(response.summary().riskLevel()).isEqualTo(OperationalUncertainty.HIGH);
		assertThat(response.summary().uncertaintyDetected()).isTrue();
		assertThat(response.summary().uncertaintyReason())
				.isEqualTo(EvidenceRuntimeSummaryReason.PAYMENT_SAFETY_UNCERTAINTY);
		assertThat(response.summary().evidenceCompleteness()).isEqualTo(EvidenceCompleteness.PARTIAL);
		assertThat(response.summary().auditTrusted()).isTrue();
	}

	@Test
	void shouldTreatAdapterFailureAsEvidenceUncertaintyNotSystemFailure() {
		EvidenceRuntimeApiResponse response = boundary.read(request(
				new EvidenceRuntimeSummaryResource(),
				summary(
						EvidenceRuntimeSummaryStatus.UNCERTAIN,
						OperationalUncertainty.HIGH,
						OperationalUncertainty.MODERATE,
						true,
						EvidenceRuntimeSummaryReason.ADAPTER_FAILURE,
						true,
						EvidenceCompleteness.PARTIAL
				)
		));

		assertThat(response.status()).isEqualTo(EvidenceRuntimeApiStatus.UNCERTAIN);
		assertThat(response.summary().uncertaintyReason())
				.isEqualTo(EvidenceRuntimeSummaryReason.ADAPTER_FAILURE);
	}

	@Test
	void shouldRejectUntrustedAudit() {
		EvidenceRuntimeApiResponse response = boundary.read(request(
				new EvidenceRuntimeSummaryResource(),
				summary(
						EvidenceRuntimeSummaryStatus.UNCERTAIN,
						OperationalUncertainty.MODERATE,
						OperationalUncertainty.MODERATE,
						true,
						EvidenceRuntimeSummaryReason.UNKNOWN_EVIDENCE,
						false,
						EvidenceCompleteness.ABSENT
				)
		));

		assertThat(response.status()).isEqualTo(EvidenceRuntimeApiStatus.UNTRUSTED);
		assertThat(response.rejectionReason())
				.isEqualTo(EvidenceRuntimeApiRejectionReason.UNTRUSTED_AUDIT);
	}

	@Test
	void shouldRejectMissingSummaryResource() {
		EvidenceRuntimeApiResponse response = boundary.read(request(
				null,
				summary(
						EvidenceRuntimeSummaryStatus.UNKNOWN,
						OperationalUncertainty.HIGH,
						OperationalUncertainty.HIGH,
						true,
						EvidenceRuntimeSummaryReason.UNKNOWN,
						false,
						EvidenceCompleteness.ABSENT
				)
		));

		assertThat(response.status()).isEqualTo(EvidenceRuntimeApiStatus.REJECTED);
		assertThat(response.rejectionReason())
				.isEqualTo(EvidenceRuntimeApiRejectionReason.MISSING_SUMMARY_RESOURCE);
	}

	@Test
	void shouldNotExposeRawPayloadVendorDetailOrCredentials() {
		EvidenceRuntimeApiResponse response = boundary.read(request(
				new EvidenceRuntimeSummaryResource(),
				summary(
						EvidenceRuntimeSummaryStatus.PARTIAL,
						OperationalUncertainty.MODERATE,
						OperationalUncertainty.LOW,
						true,
						EvidenceRuntimeSummaryReason.PARTIAL_EVIDENCE,
						true,
						EvidenceCompleteness.PARTIAL
				)
		));

		assertThat(response.exposesRawPayload()).isFalse();
		assertThat(response.exposesVendorDetail()).isFalse();
		assertThat(response.exposesCredentialConfiguration()).isFalse();
	}

	@Test
	void shouldNotMutatePortfolioKnowledgeSourceAndShouldDeferAuth() {
		EvidenceRuntimeApiRequest request = request(
				new EvidenceRuntimeSummaryResource(),
				summary(
						EvidenceRuntimeSummaryStatus.HEALTHY,
						OperationalUncertainty.LOW,
						OperationalUncertainty.LOW,
						false,
						EvidenceRuntimeSummaryReason.UNKNOWN,
						true,
						EvidenceCompleteness.COMPLETE
				)
		);

		assertThat(request.authenticationAuthorizationDeferred()).isTrue();
		assertThat(request.mutatesPortfolioKnowledgeSource()).isFalse();
		assertThat(boundary.mutatesPortfolioKnowledgeSource()).isFalse();
	}

	@Test
	void shouldRejectNullRequest() {
		assertThatThrownBy(() -> boundary.read(null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("request must not be null");
	}

	private EvidenceRuntimeApiRequest request(
			EvidenceRuntimeSummaryResource resource,
			EvidenceRuntimeSummary summary
	) {
		return new EvidenceRuntimeApiRequest(resource, summary);
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
