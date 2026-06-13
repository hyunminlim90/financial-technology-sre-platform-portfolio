package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class OperationalReliabilityEvidenceLineageIntegrationTest {

	private final EvidenceLineageIntegration integration =
			new EvidenceLineageIntegration();
	private final EvidenceRuntimeApiBoundary apiBoundary =
			new EvidenceRuntimeApiBoundary();

	@Test
	void shouldRemainReadOnlyAndNonMutating() {
		assertThat(integration.readOnly()).isTrue();
		assertThat(integration.mutatesEvidence()).isFalse();
		assertThat(integration.recommendationAuthority()).isFalse();
		assertThat(integration.executionAuthority()).isFalse();
		assertThat(integration.mutatesPortfolioKnowledgeSource()).isFalse();
	}

	@Test
	void shouldPreventTrustedSummaryForIncompleteLineage() {
		EvidenceLineageIntegrationResult result = integration.integrate(
				apiBoundary,
				apiRequest(summary(true)),
				lineage(
						EvidenceLineageStatus.INCOMPLETE,
						EvidenceLineageReason.MISSING_COLLECTION_STAGE,
						governancePolicy(
								EvidenceTrustLevel.TRUSTED,
								EvidenceIntegrityStatus.INTACT,
								EvidenceClassification.PUBLIC_SAFE,
								provenance(true, false, false)
						),
						OperationalUncertainty.HIGH
				)
		);

		assertThat(result.status()).isEqualTo(EvidenceLineageIntegrationStatus.UNTRUSTED);
		assertThat(result.apiResponse().status()).isEqualTo(EvidenceRuntimeApiStatus.UNTRUSTED);
		assertThat(result.apiResponse().summary().auditTrusted()).isFalse();
	}

	@Test
	void shouldBlockApiExposureForBlockedLineage() {
		EvidenceLineageIntegrationResult result = integration.integrate(
				apiBoundary,
				apiRequest(summary(true)),
				lineage(
						EvidenceLineageStatus.BLOCKED,
						EvidenceLineageReason.BLOCKED_EVIDENCE,
						governancePolicy(
								EvidenceTrustLevel.PARTIALLY_TRUSTED,
								EvidenceIntegrityStatus.DEGRADED,
								EvidenceClassification.BLOCKED,
								provenance(true, false, true)
						),
						OperationalUncertainty.CRITICAL
				)
		);

		assertThat(result.status()).isEqualTo(EvidenceLineageIntegrationStatus.BLOCKED);
		assertThat(result.reason()).isEqualTo(EvidenceLineageIntegrationReason.BLOCKED_LINEAGE);
	}

	@Test
	void shouldRestrictOperatorFacingExposureForRestrictedLineage() {
		EvidenceLineageIntegrationResult result = integration.integrate(
				apiBoundary,
				apiRequest(summary(true)),
				lineage(
						EvidenceLineageStatus.RESTRICTED,
						EvidenceLineageReason.GOVERNANCE_PROTECTED_EVIDENCE,
						governancePolicy(
								EvidenceTrustLevel.TRUSTED,
								EvidenceIntegrityStatus.DEGRADED,
								EvidenceClassification.GOVERNANCE_PROTECTED,
								provenance(true, true, false)
						),
						OperationalUncertainty.HIGH
				)
		);

		assertThat(result.status()).isEqualTo(EvidenceLineageIntegrationStatus.RESTRICTED);
		assertThat(result.reason()).isEqualTo(EvidenceLineageIntegrationReason.RESTRICTED_LINEAGE);
		assertThat(result.apiResponse().status()).isEqualTo(EvidenceRuntimeApiStatus.UNCERTAIN);
	}

	@Test
	void shouldPropagateContradictoryLineageRiskAsUncertainty() {
		EvidenceLineageIntegrationResult result = integration.integrate(
				apiBoundary,
				apiRequest(summary(true)),
				lineage(
						EvidenceLineageStatus.PARTIAL,
						EvidenceLineageReason.CONTRADICTORY_EVIDENCE,
						governancePolicy(
								EvidenceTrustLevel.TRUSTED,
								EvidenceIntegrityStatus.CONTRADICTORY,
								EvidenceClassification.PUBLIC_SAFE,
								provenance(true, false, false)
						),
						OperationalUncertainty.HIGH
				)
		);

		assertThat(result.status()).isEqualTo(EvidenceLineageIntegrationStatus.RESTRICTED);
		assertThat(result.reason())
				.isEqualTo(EvidenceLineageIntegrationReason.CONTRADICTORY_LINEAGE_RISK);
		assertThat(result.apiResponse().summary().riskLevel())
				.isEqualTo(OperationalUncertainty.HIGH);
		assertThat(result.apiResponse().summary().uncertaintyDetected()).isTrue();
	}

	@Test
	void shouldPropagatePaymentLineageAsRestrictedPaymentSafetyState() {
		EvidenceLineageIntegrationResult result = integration.integrate(
				apiBoundary,
				apiRequest(summary(true)),
				lineage(
						EvidenceLineageStatus.RESTRICTED,
						EvidenceLineageReason.PAYMENT_RESTRICTED_EVIDENCE,
						governancePolicy(
								EvidenceTrustLevel.TRUSTED,
								EvidenceIntegrityStatus.INTACT,
								EvidenceClassification.RESTRICTED,
								provenance(true, false, false)
						),
						OperationalUncertainty.HIGH
				)
		);

		assertThat(result.reason())
				.isEqualTo(EvidenceLineageIntegrationReason.PAYMENT_LINEAGE_RESTRICTED);
		assertThat(result.apiResponse().summary().paymentSafetyState())
				.isEqualTo(OperationalUncertainty.HIGH);
	}

	@Test
	void shouldPropagateMissingProvenanceLineageAsTrustDowngrade() {
		EvidenceLineageIntegrationResult result = integration.integrate(
				apiBoundary,
				apiRequest(summary(true)),
				lineage(
						EvidenceLineageStatus.INCOMPLETE,
						EvidenceLineageReason.MISSING_PROVENANCE,
						governancePolicy(
								EvidenceTrustLevel.UNTRUSTED,
								EvidenceIntegrityStatus.MISSING,
								EvidenceClassification.UNKNOWN,
								EvidenceProvenance.missingProvenance()
						),
						OperationalUncertainty.HIGH
				)
		);

		assertThat(result.scope()).isEqualTo(EvidenceLineageIntegrationScope.TRUST_DOWNGRADED);
		assertThat(result.reason())
				.isEqualTo(EvidenceLineageIntegrationReason.MISSING_PROVENANCE_LINEAGE);
	}

	@Test
	void shouldRemainNonRecommendationAndNonExecutionAuthority() {
		EvidenceLineageIntegrationResult result = integration.integrate(
				apiBoundary,
				apiRequest(summary(true)),
				lineage(
						EvidenceLineageStatus.COMPLETE,
						EvidenceLineageReason.UNKNOWN,
						governancePolicy(
								EvidenceTrustLevel.TRUSTED,
								EvidenceIntegrityStatus.INTACT,
								EvidenceClassification.PUBLIC_SAFE,
								provenance(true, false, false)
						),
						OperationalUncertainty.LOW
				)
		);

		assertThat(result.readOnly()).isTrue();
		assertThat(result.recommendationAuthority()).isFalse();
		assertThat(result.executionAuthority()).isFalse();
	}

	@Test
	void shouldRejectNullArguments() {
		assertThatThrownBy(() -> integration.integrate(
				null,
				apiRequest(summary(true)),
				lineage(
						EvidenceLineageStatus.COMPLETE,
						EvidenceLineageReason.UNKNOWN,
						governancePolicy(
								EvidenceTrustLevel.TRUSTED,
								EvidenceIntegrityStatus.INTACT,
								EvidenceClassification.PUBLIC_SAFE,
								provenance(true, false, false)
						),
						OperationalUncertainty.LOW
				)
		))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("apiBoundary must not be null");
		assertThatThrownBy(() -> integration.integrate(
				apiBoundary,
				null,
				lineage(
						EvidenceLineageStatus.COMPLETE,
						EvidenceLineageReason.UNKNOWN,
						governancePolicy(
								EvidenceTrustLevel.TRUSTED,
								EvidenceIntegrityStatus.INTACT,
								EvidenceClassification.PUBLIC_SAFE,
								provenance(true, false, false)
						),
						OperationalUncertainty.LOW
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
				.hasMessage("lineage must not be null");
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

	private EvidenceLineage lineage(
			EvidenceLineageStatus status,
			EvidenceLineageReason reason,
			EvidenceGovernancePolicy governancePolicy,
			OperationalUncertainty riskLevel
	) {
		return new EvidenceLineage(
				java.util.List.of(
						EvidenceLineageNode.SOURCE,
						EvidenceLineageNode.ADAPTER,
						EvidenceLineageNode.ROUTING,
						EvidenceLineageNode.DISPATCH,
						EvidenceLineageNode.EXECUTION,
						EvidenceLineageNode.COLLECTION,
						EvidenceLineageNode.ASSESSMENT,
						EvidenceLineageNode.SUMMARY
				),
				java.util.List.of(
						new EvidenceLineageEdge(EvidenceLineageNode.SOURCE, EvidenceLineageNode.ADAPTER),
						new EvidenceLineageEdge(EvidenceLineageNode.ADAPTER, EvidenceLineageNode.ROUTING),
						new EvidenceLineageEdge(EvidenceLineageNode.ROUTING, EvidenceLineageNode.DISPATCH),
						new EvidenceLineageEdge(EvidenceLineageNode.DISPATCH, EvidenceLineageNode.EXECUTION),
						new EvidenceLineageEdge(EvidenceLineageNode.EXECUTION, EvidenceLineageNode.COLLECTION),
						new EvidenceLineageEdge(EvidenceLineageNode.COLLECTION, EvidenceLineageNode.ASSESSMENT),
						new EvidenceLineageEdge(EvidenceLineageNode.ASSESSMENT, EvidenceLineageNode.SUMMARY)
				),
				status,
				reason,
				governancePolicy,
				riskLevel
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
				Instant.parse("2026-06-01T00:00:00Z"),
				sanitized,
				rawPayloadPresent,
				sensitiveDataPresent
		);
	}
}
