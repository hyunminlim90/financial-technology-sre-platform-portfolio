package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class OperationalReliabilityEvidenceTrustIntegrationTest {

	private final EvidenceTrustIntegration integration =
			new EvidenceTrustIntegration();

	@Test
	void shouldRemainReadOnlyAndNonMutating() {
		assertThat(integration.readOnly()).isTrue();
		assertThat(integration.mutatesEvidence()).isFalse();
		assertThat(integration.recommendationAuthority()).isFalse();
		assertThat(integration.executionAuthority()).isFalse();
		assertThat(integration.mutatesPortfolioKnowledgeSource()).isFalse();
	}

	@Test
	void shouldRejectTrustedSummaryForUntrustedScore() {
		EvidenceTrustIntegrationResult result = integration.integrate(trustScore(
				EvidenceTrustScoreLevel.UNTRUSTED,
				EvidenceTrustScoreReason.BLOCKED_EVIDENCE,
				EvidenceTrustScoreScope.API_BOUNDARY,
				governancePolicy(
						EvidenceTrustLevel.UNTRUSTED,
						EvidenceIntegrityStatus.DEGRADED,
						EvidenceClassification.BLOCKED,
						provenance(true, false, true)
				),
				lineage(
						EvidenceLineageStatus.BLOCKED,
						EvidenceLineageReason.BLOCKED_EVIDENCE,
						governancePolicy(
								EvidenceTrustLevel.UNTRUSTED,
								EvidenceIntegrityStatus.DEGRADED,
								EvidenceClassification.BLOCKED,
								provenance(true, false, true)
						),
						OperationalUncertainty.CRITICAL
				)
		));

		assertThat(result.status()).isEqualTo(EvidenceTrustIntegrationStatus.UNTRUSTED);
		assertThat(result.apiResponse().status()).isEqualTo(EvidenceRuntimeApiStatus.REJECTED);
	}

	@Test
	void shouldRequireOperatorFacingWarningForLowScore() {
		EvidenceTrustIntegrationResult result = integration.integrate(trustScore(
				EvidenceTrustScoreLevel.LOW,
				EvidenceTrustScoreReason.INCOMPLETE_LINEAGE,
				EvidenceTrustScoreScope.OPERATOR_VIEW,
				governancePolicy(
						EvidenceTrustLevel.TRUSTED,
						EvidenceIntegrityStatus.INTACT,
						EvidenceClassification.PUBLIC_SAFE,
						provenance(true, false, false)
				),
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
		));

		assertThat(result.status()).isEqualTo(EvidenceTrustIntegrationStatus.WARNING);
		assertThat(result.reason()).isEqualTo(EvidenceTrustIntegrationReason.LOW_TRUST_SCORE);
		assertThat(result.scope()).isEqualTo(EvidenceTrustIntegrationScope.OPERATOR_WARNING_VIEW);
		assertThat(result.apiResponse().summary().uncertaintyDetected()).isTrue();
	}

	@Test
	void shouldMarkMediumScoreAsPartialTrust() {
		EvidenceTrustIntegrationResult result = integration.integrate(trustScore(
				EvidenceTrustScoreLevel.MEDIUM,
				EvidenceTrustScoreReason.PARTIAL_PROVENANCE,
				EvidenceTrustScoreScope.OPERATOR_VIEW,
				governancePolicy(
						EvidenceTrustLevel.PARTIALLY_TRUSTED,
						EvidenceIntegrityStatus.INTACT,
						EvidenceClassification.INTERNAL,
						provenance(false, false, false)
				),
				lineage(
						EvidenceLineageStatus.COMPLETE,
						EvidenceLineageReason.UNKNOWN,
						governancePolicy(
								EvidenceTrustLevel.PARTIALLY_TRUSTED,
								EvidenceIntegrityStatus.INTACT,
								EvidenceClassification.INTERNAL,
								provenance(false, false, false)
						),
						OperationalUncertainty.MODERATE
				)
		));

		assertThat(result.status()).isEqualTo(EvidenceTrustIntegrationStatus.PARTIAL_TRUST);
		assertThat(result.reason()).isEqualTo(EvidenceTrustIntegrationReason.MEDIUM_TRUST_SCORE);
	}

	@Test
	void shouldAllowOnlyHighScoreAsTrustedEvidenceViewCandidate() {
		EvidenceTrustIntegrationResult result = integration.integrate(trustScore(
				EvidenceTrustScoreLevel.HIGH,
				EvidenceTrustScoreReason.TRUSTED_PROVENANCE,
				EvidenceTrustScoreScope.EVIDENCE,
				governancePolicy(
						EvidenceTrustLevel.TRUSTED,
						EvidenceIntegrityStatus.INTACT,
						EvidenceClassification.PUBLIC_SAFE,
						provenance(true, false, false)
				),
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
		));

		assertThat(result.status()).isEqualTo(EvidenceTrustIntegrationStatus.TRUSTED);
		assertThat(result.reason()).isEqualTo(EvidenceTrustIntegrationReason.HIGH_TRUST_SCORE);
		assertThat(result.scope()).isEqualTo(EvidenceTrustIntegrationScope.TRUSTED_EVIDENCE_VIEW);
	}

	@Test
	void shouldMaintainPaymentTrustRestriction() {
		EvidenceTrustIntegrationResult result = integration.integrate(trustScore(
				EvidenceTrustScoreLevel.MEDIUM,
				EvidenceTrustScoreReason.PAYMENT_RESTRICTED_EVIDENCE,
				EvidenceTrustScoreScope.PAYMENT_EVIDENCE,
				governancePolicy(
						EvidenceTrustLevel.TRUSTED,
						EvidenceIntegrityStatus.INTACT,
						EvidenceClassification.RESTRICTED,
						provenance(true, false, false)
				),
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
		));

		assertThat(result.reason())
				.isEqualTo(EvidenceTrustIntegrationReason.PAYMENT_TRUST_RESTRICTED);
		assertThat(result.scope())
				.isEqualTo(EvidenceTrustIntegrationScope.PAYMENT_RESTRICTED_VIEW);
		assertThat(result.apiResponse().summary().paymentSafetyState())
				.isEqualTo(OperationalUncertainty.HIGH);
	}

	@Test
	void shouldKeepBlockedEvidenceHiddenFromApiResponse() {
		EvidenceTrustIntegrationResult result = integration.integrate(trustScore(
				EvidenceTrustScoreLevel.UNTRUSTED,
				EvidenceTrustScoreReason.BLOCKED_EVIDENCE,
				EvidenceTrustScoreScope.API_BOUNDARY,
				governancePolicy(
						EvidenceTrustLevel.UNTRUSTED,
						EvidenceIntegrityStatus.DEGRADED,
						EvidenceClassification.BLOCKED,
						provenance(true, false, true)
				),
				lineage(
						EvidenceLineageStatus.BLOCKED,
						EvidenceLineageReason.BLOCKED_EVIDENCE,
						governancePolicy(
								EvidenceTrustLevel.UNTRUSTED,
								EvidenceIntegrityStatus.DEGRADED,
								EvidenceClassification.BLOCKED,
								provenance(true, false, true)
						),
						OperationalUncertainty.CRITICAL
				)
		));

		assertThat(result.apiResponse().status()).isEqualTo(EvidenceRuntimeApiStatus.REJECTED);
	}

	@Test
	void shouldRemainNonRecommendationAndNonExecutionAuthority() {
		EvidenceTrustIntegrationResult result = integration.integrate(trustScore(
				EvidenceTrustScoreLevel.HIGH,
				EvidenceTrustScoreReason.TRUSTED_PROVENANCE,
				EvidenceTrustScoreScope.EVIDENCE,
				governancePolicy(
						EvidenceTrustLevel.TRUSTED,
						EvidenceIntegrityStatus.INTACT,
						EvidenceClassification.PUBLIC_SAFE,
						provenance(true, false, false)
				),
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
		));

		assertThat(result.recommendationAuthority()).isFalse();
		assertThat(result.executionAuthority()).isFalse();
		assertThat(result.readOnly()).isTrue();
	}

	@Test
	void shouldRejectNullTrustScore() {
		assertThatThrownBy(() -> integration.integrate(null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("trustScore must not be null");
	}

	private EvidenceTrustScore trustScore(
			EvidenceTrustScoreLevel level,
			EvidenceTrustScoreReason reason,
			EvidenceTrustScoreScope scope,
			EvidenceGovernancePolicy policy,
			EvidenceLineage lineage
	) {
		return new EvidenceTrustScore(
				level,
				reason,
				scope,
				new EvidenceGovernanceIntegrationResult(
						policy,
						apiResponse(OperationalUncertainty.LOW, OperationalUncertainty.HIGH, true),
						EvidenceGovernanceIntegrationStatus.INTEGRATED,
						EvidenceGovernanceIntegrationReason.UNKNOWN,
						EvidenceGovernanceIntegrationScope.API_EXPOSURE
				),
				new EvidenceLineageIntegrationResult(
						lineage,
						apiResponse(lineage.riskLevel(),
								policy.classification() == EvidenceClassification.RESTRICTED
										? OperationalUncertainty.HIGH
										: OperationalUncertainty.LOW,
								lineage.status() != EvidenceLineageStatus.INCOMPLETE),
						lineage.status() == EvidenceLineageStatus.BLOCKED
								? EvidenceLineageIntegrationStatus.BLOCKED
								: lineage.status() == EvidenceLineageStatus.INCOMPLETE
										? EvidenceLineageIntegrationStatus.UNTRUSTED
										: lineage.status() == EvidenceLineageStatus.RESTRICTED
												? EvidenceLineageIntegrationStatus.RESTRICTED
												: EvidenceLineageIntegrationStatus.INTEGRATED,
						lineage.reason() == EvidenceLineageReason.BLOCKED_EVIDENCE
								? EvidenceLineageIntegrationReason.BLOCKED_LINEAGE
								: lineage.reason() == EvidenceLineageReason.MISSING_PROVENANCE
										? EvidenceLineageIntegrationReason.MISSING_PROVENANCE_LINEAGE
										: lineage.reason() == EvidenceLineageReason.PAYMENT_RESTRICTED_EVIDENCE
												? EvidenceLineageIntegrationReason.PAYMENT_LINEAGE_RESTRICTED
												: lineage.reason() == EvidenceLineageReason.CONTRADICTORY_EVIDENCE
														? EvidenceLineageIntegrationReason.CONTRADICTORY_LINEAGE_RISK
														: EvidenceLineageIntegrationReason.UNKNOWN,
						lineage.status() == EvidenceLineageStatus.BLOCKED
								? EvidenceLineageIntegrationScope.API_BLOCKED
								: lineage.status() == EvidenceLineageStatus.INCOMPLETE
										? EvidenceLineageIntegrationScope.TRUST_DOWNGRADED
										: lineage.status() == EvidenceLineageStatus.RESTRICTED
												? EvidenceLineageIntegrationScope.OPERATOR_FACING_RESTRICTED
												: EvidenceLineageIntegrationScope.API_EXPOSURE
				)
		);
	}

	private EvidenceRuntimeApiResponse apiResponse(
			OperationalUncertainty risk,
			OperationalUncertainty paymentSafety,
			boolean auditTrusted
	) {
		return new EvidenceRuntimeApiResponse(
				new EvidenceRuntimeSummaryView(
						EvidenceRuntimeSummaryStatus.HEALTHY,
						risk,
						paymentSafety,
						false,
						EvidenceRuntimeSummaryReason.UNKNOWN,
						auditTrusted,
						EvidenceCompleteness.COMPLETE
				),
				EvidenceRuntimeApiStatus.READABLE,
				EvidenceRuntimeApiRejectionReason.UNKNOWN
		);
	}

	private EvidenceLineage lineage(
			EvidenceLineageStatus status,
			EvidenceLineageReason reason,
			EvidenceGovernancePolicy policy,
			OperationalUncertainty risk
	) {
		return new EvidenceLineage(
				List.of(
						EvidenceLineageNode.SOURCE,
						EvidenceLineageNode.ADAPTER,
						EvidenceLineageNode.ROUTING,
						EvidenceLineageNode.DISPATCH,
						EvidenceLineageNode.EXECUTION,
						EvidenceLineageNode.COLLECTION,
						EvidenceLineageNode.ASSESSMENT,
						EvidenceLineageNode.SUMMARY
				),
				List.of(
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
				policy,
				risk
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
