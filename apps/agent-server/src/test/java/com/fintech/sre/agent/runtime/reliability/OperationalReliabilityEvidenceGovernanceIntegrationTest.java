package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class OperationalReliabilityEvidenceGovernanceIntegrationTest {

	private final EvidenceGovernanceIntegration integration =
			new EvidenceGovernanceIntegration();
	private final EvidenceRuntimeApiBoundary apiBoundary =
			new EvidenceRuntimeApiBoundary();

	@Test
	void shouldRemainNonMutatingAndNonAuthoritative() {
		assertThat(integration.mutatesEvidence()).isFalse();
		assertThat(integration.recommendationAuthority()).isFalse();
		assertThat(integration.executionAuthority()).isFalse();
		assertThat(integration.mutatesPortfolioKnowledgeSource()).isFalse();
	}

	@Test
	void shouldNotAllowTrustedApiExposureForUntrustedEvidence() {
		EvidenceGovernanceIntegrationResult result = integration.integrate(
				apiBoundary,
				apiRequest(summary(true)),
				governancePolicy(
						EvidenceTrustLevel.UNTRUSTED,
						EvidenceIntegrityStatus.MISSING,
						EvidenceClassification.UNKNOWN,
						EvidenceProvenance.missingProvenance()
				)
		);

		assertThat(result.status()).isEqualTo(EvidenceGovernanceIntegrationStatus.UNTRUSTED);
		assertThat(result.apiResponse().status()).isEqualTo(EvidenceRuntimeApiStatus.UNTRUSTED);
		assertThat(result.apiResponse().summary().auditTrusted()).isFalse();
	}

	@Test
	void shouldBlockApiExposureForBlockedEvidence() {
		EvidenceGovernanceIntegrationResult result = integration.integrate(
				apiBoundary,
				apiRequest(summary(true)),
				governancePolicy(
						EvidenceTrustLevel.PARTIALLY_TRUSTED,
						EvidenceIntegrityStatus.DEGRADED,
						EvidenceClassification.BLOCKED,
						provenance(true, false, true)
				)
		);

		assertThat(result.status()).isEqualTo(EvidenceGovernanceIntegrationStatus.BLOCKED);
		assertThat(result.reason())
				.isEqualTo(EvidenceGovernanceIntegrationReason.BLOCKED_CLASSIFICATION);
	}

	@Test
	void shouldRestrictGovernanceProtectedEvidenceForOperatorFacingExposure() {
		EvidenceGovernanceIntegrationResult result = integration.integrate(
				apiBoundary,
				apiRequest(summary(true)),
				governancePolicy(
						EvidenceTrustLevel.TRUSTED,
						EvidenceIntegrityStatus.DEGRADED,
						EvidenceClassification.GOVERNANCE_PROTECTED,
						provenance(true, true, false)
				)
		);

		assertThat(result.status()).isEqualTo(EvidenceGovernanceIntegrationStatus.RESTRICTED);
		assertThat(result.reason()).isEqualTo(
				EvidenceGovernanceIntegrationReason.GOVERNANCE_PROTECTED_CLASSIFICATION
		);
	}

	@Test
	void shouldPropagateContradictoryIntegrityAsUncertaintyAndRisk() {
		EvidenceGovernanceIntegrationResult result = integration.integrate(
				apiBoundary,
				apiRequest(summary(true)),
				governancePolicy(
						EvidenceTrustLevel.TRUSTED,
						EvidenceIntegrityStatus.CONTRADICTORY,
						EvidenceClassification.PUBLIC_SAFE,
						provenance(true, false, false)
				)
		);

		assertThat(result.status()).isEqualTo(EvidenceGovernanceIntegrationStatus.RESTRICTED);
		assertThat(result.reason())
				.isEqualTo(EvidenceGovernanceIntegrationReason.CONTRADICTORY_INTEGRITY);
		assertThat(result.apiResponse().summary().riskLevel())
				.isEqualTo(OperationalUncertainty.HIGH);
		assertThat(result.apiResponse().summary().uncertaintyDetected()).isTrue();
	}

	@Test
	void shouldReflectRestrictedPaymentEvidenceInPaymentSafetyState() {
		EvidenceGovernanceIntegrationResult result = integration.integrate(
				apiBoundary,
				apiRequest(summary(true)),
				governancePolicy(
						EvidenceTrustLevel.TRUSTED,
						EvidenceIntegrityStatus.INTACT,
						EvidenceClassification.RESTRICTED,
						provenance(true, false, false)
				)
		);

		assertThat(result.status()).isEqualTo(EvidenceGovernanceIntegrationStatus.RESTRICTED);
		assertThat(result.reason()).isEqualTo(
				EvidenceGovernanceIntegrationReason.PAYMENT_RESTRICTED_CLASSIFICATION
		);
		assertThat(result.apiResponse().summary().paymentSafetyState())
				.isEqualTo(OperationalUncertainty.HIGH);
	}

	@Test
	void shouldRestrictUnsanitizedEvidenceBeforeApiBoundaryExposure() {
		EvidenceGovernanceIntegrationResult result = integration.integrate(
				apiBoundary,
				apiRequest(summary(true)),
				governancePolicy(
						EvidenceTrustLevel.PARTIALLY_TRUSTED,
						EvidenceIntegrityStatus.INTACT,
						EvidenceClassification.INTERNAL,
						provenance(false, false, false)
				)
		);

		assertThat(result.status()).isEqualTo(EvidenceGovernanceIntegrationStatus.RESTRICTED);
		assertThat(result.reason())
				.isEqualTo(EvidenceGovernanceIntegrationReason.UNSANITIZED_EVIDENCE);
	}

	@Test
	void shouldPropagateMissingProvenanceAsTrustDowngrade() {
		EvidenceGovernanceIntegrationResult result = integration.integrate(
				apiBoundary,
				apiRequest(summary(true)),
				governancePolicy(
						EvidenceTrustLevel.UNTRUSTED,
						EvidenceIntegrityStatus.MISSING,
						EvidenceClassification.UNKNOWN,
						EvidenceProvenance.missingProvenance()
				)
		);

		assertThat(result.scope()).isEqualTo(EvidenceGovernanceIntegrationScope.TRUST_DOWNGRADED);
		assertThat(result.reason()).isEqualTo(EvidenceGovernanceIntegrationReason.MISSING_PROVENANCE);
	}

	@Test
	void shouldRemainNonRecommendationAndNonExecutionPermission() {
		EvidenceGovernanceIntegrationResult result = integration.integrate(
				apiBoundary,
				apiRequest(summary(true)),
				governancePolicy(
						EvidenceTrustLevel.TRUSTED,
						EvidenceIntegrityStatus.INTACT,
						EvidenceClassification.PUBLIC_SAFE,
						provenance(true, false, false)
				)
		);

		assertThat(result.recommendation()).isFalse();
		assertThat(result.executionPermission()).isFalse();
	}

	@Test
	void shouldRejectNullArguments() {
		assertThatThrownBy(() -> integration.integrate(
				null,
				apiRequest(summary(true)),
				governancePolicy(
						EvidenceTrustLevel.TRUSTED,
						EvidenceIntegrityStatus.INTACT,
						EvidenceClassification.PUBLIC_SAFE,
						provenance(true, false, false)
				)
		))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("apiBoundary must not be null");
		assertThatThrownBy(() -> integration.integrate(
				apiBoundary,
				null,
				governancePolicy(
						EvidenceTrustLevel.TRUSTED,
						EvidenceIntegrityStatus.INTACT,
						EvidenceClassification.PUBLIC_SAFE,
						provenance(true, false, false)
				)
		))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("apiRequest must not be null");
		assertThatThrownBy(() -> integration.integrate(
				apiBoundary,
				apiRequest(summary(true)),
				null
		))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("governancePolicy must not be null");
	}

	private EvidenceRuntimeApiRequest apiRequest(EvidenceRuntimeSummary summary) {
		return new EvidenceRuntimeApiRequest(
				new EvidenceRuntimeSummaryResource(),
				summary
		);
	}

	private EvidenceRuntimeSummary summary(boolean auditTrusted) {
		return new EvidenceRuntimeSummary(
				EvidenceRuntimeSummaryStatus.HEALTHY,
				OperationalUncertainty.LOW,
				OperationalUncertainty.HIGH,
				false,
				EvidenceRuntimeSummaryReason.UNKNOWN,
				auditTrusted,
				EvidenceCompleteness.COMPLETE
		);
	}

	private EvidenceGovernancePolicy governancePolicy(
			EvidenceTrustLevel trustLevel,
			EvidenceIntegrityStatus integrityStatus,
			EvidenceClassification classification,
			EvidenceProvenance provenance
	) {
		return new EvidenceGovernancePolicy(
				trustLevel,
				integrityStatus,
				classification,
				provenance
		);
	}

	private EvidenceProvenance provenance(
			boolean sanitized,
			boolean rawPayloadPresent,
			boolean sensitiveDataPresent
	) {
		return new EvidenceProvenance(
				EvidenceSourceType.METRICS,
				"adapter-1",
				Instant.parse("2026-05-31T00:00:00Z"),
				sanitized,
				rawPayloadPresent,
				sensitiveDataPresent
		);
	}
}
