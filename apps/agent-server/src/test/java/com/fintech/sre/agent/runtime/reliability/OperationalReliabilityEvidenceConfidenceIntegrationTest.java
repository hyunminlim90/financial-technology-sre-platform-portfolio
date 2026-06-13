package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class OperationalReliabilityEvidenceConfidenceIntegrationTest {

	private final EvidenceConfidenceIntegration integration =
			new EvidenceConfidenceIntegration();

	@Test
	void shouldRemainReadOnlyAndNonMutating() {
		assertThat(integration.readOnly()).isTrue();
		assertThat(integration.mutatesEvidence()).isFalse();
		assertThat(integration.recommendationAuthority()).isFalse();
		assertThat(integration.executionAuthority()).isFalse();
		assertThat(integration.mutatesPortfolioKnowledgeSource()).isFalse();
	}

	@Test
	void shouldPreventAssessmentCertaintyForInsufficientConfidence() {
		EvidenceConfidenceIntegrationResult result = integration.integrate(confidence(
				EvidenceConfidenceLevel.INSUFFICIENT,
				EvidenceConfidenceReason.INSUFFICIENT_EVIDENCE,
				EvidenceConfidenceScope.ASSESSMENT,
				trustScore(
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
				)
		));

		assertThat(result.status()).isEqualTo(EvidenceConfidenceIntegrationStatus.INSUFFICIENT);
		assertThat(result.scope())
				.isEqualTo(EvidenceConfidenceIntegrationScope.ASSESSMENT_UNCERTAINTY);
		assertThat(result.apiResponse().status()).isEqualTo(EvidenceRuntimeApiStatus.UNTRUSTED);
	}

	@Test
	void shouldRequireOperatorWarningForLowConfidence() {
		EvidenceConfidenceIntegrationResult result = integration.integrate(confidence(
				EvidenceConfidenceLevel.LOW,
				EvidenceConfidenceReason.PARTIAL_EVIDENCE,
				EvidenceConfidenceScope.OPERATOR_VIEW,
				trustScore(
						EvidenceTrustScoreLevel.LOW,
						EvidenceTrustScoreReason.DEGRADED_INTEGRITY,
						EvidenceTrustScoreScope.EVIDENCE,
						governancePolicy(
								EvidenceTrustLevel.TRUSTED,
								EvidenceIntegrityStatus.DEGRADED,
								EvidenceClassification.PUBLIC_SAFE,
								provenance(true, false, false)
						),
						lineage(
								EvidenceLineageStatus.COMPLETE,
								EvidenceLineageReason.UNKNOWN,
								governancePolicy(
										EvidenceTrustLevel.TRUSTED,
										EvidenceIntegrityStatus.DEGRADED,
										EvidenceClassification.PUBLIC_SAFE,
										provenance(true, false, false)
								),
								OperationalUncertainty.MODERATE
						)
				)
		));

		assertThat(result.status()).isEqualTo(EvidenceConfidenceIntegrationStatus.WARNING);
		assertThat(result.reason()).isEqualTo(EvidenceConfidenceIntegrationReason.LOW_CONFIDENCE);
		assertThat(result.scope()).isEqualTo(EvidenceConfidenceIntegrationScope.OPERATOR_WARNING_VIEW);
	}

	@Test
	void shouldMarkMediumConfidenceAsPartialConfidence() {
		EvidenceConfidenceIntegrationResult result = integration.integrate(confidence(
				EvidenceConfidenceLevel.MEDIUM,
				EvidenceConfidenceReason.PARTIAL_EVIDENCE,
				EvidenceConfidenceScope.OPERATOR_VIEW,
				trustScore(
						EvidenceTrustScoreLevel.MEDIUM,
						EvidenceTrustScoreReason.PARTIAL_PROVENANCE,
						EvidenceTrustScoreScope.EVIDENCE,
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
				)
		));

		assertThat(result.status()).isEqualTo(EvidenceConfidenceIntegrationStatus.PARTIAL_CONFIDENCE);
		assertThat(result.reason()).isEqualTo(EvidenceConfidenceIntegrationReason.MEDIUM_CONFIDENCE);
	}

	@Test
	void shouldAllowOnlyHighConfidenceAsConfidentEvidenceViewCandidate() {
		EvidenceConfidenceIntegrationResult result = integration.integrate(confidence(
				EvidenceConfidenceLevel.HIGH,
				EvidenceConfidenceReason.CORROBORATING_EVIDENCE,
				EvidenceConfidenceScope.EVIDENCE,
				trustScore(
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
				)
		));

		assertThat(result.status()).isEqualTo(EvidenceConfidenceIntegrationStatus.CONFIDENT);
		assertThat(result.reason()).isEqualTo(EvidenceConfidenceIntegrationReason.HIGH_CONFIDENCE);
		assertThat(result.scope()).isEqualTo(EvidenceConfidenceIntegrationScope.CONFIDENT_EVIDENCE_VIEW);
	}

	@Test
	void shouldPropagatePaymentConfidenceDowngradeAsPaymentSafetyUncertainty() {
		EvidenceConfidenceIntegrationResult result = integration.integrate(confidence(
				EvidenceConfidenceLevel.INSUFFICIENT,
				EvidenceConfidenceReason.PAYMENT_EVIDENCE_MISSING,
				EvidenceConfidenceScope.PAYMENT_EVIDENCE,
				trustScore(
						EvidenceTrustScoreLevel.MEDIUM,
						EvidenceTrustScoreReason.PAYMENT_RESTRICTED_EVIDENCE,
						EvidenceTrustScoreScope.PAYMENT_EVIDENCE,
						governancePolicy(
								EvidenceTrustLevel.TRUSTED,
								EvidenceIntegrityStatus.INTACT,
								EvidenceClassification.RESTRICTED,
								EvidenceProvenance.missingProvenance()
						),
						lineage(
								EvidenceLineageStatus.INCOMPLETE,
								EvidenceLineageReason.MISSING_PROVENANCE,
								governancePolicy(
										EvidenceTrustLevel.TRUSTED,
										EvidenceIntegrityStatus.INTACT,
										EvidenceClassification.RESTRICTED,
										EvidenceProvenance.missingProvenance()
								),
								OperationalUncertainty.HIGH
						)
				)
		));

		assertThat(result.reason())
				.isEqualTo(EvidenceConfidenceIntegrationReason.PAYMENT_CONFIDENCE_DOWNGRADE);
		assertThat(result.scope())
				.isEqualTo(EvidenceConfidenceIntegrationScope.PAYMENT_UNCERTAINTY_VIEW);
		assertThat(result.apiResponse().summary().paymentSafetyState())
				.isEqualTo(OperationalUncertainty.HIGH);
	}

	@Test
	void shouldPropagateContradictoryEvidenceConfidenceAsRiskAndUncertainty() {
		EvidenceConfidenceIntegrationResult result = integration.integrate(confidence(
				EvidenceConfidenceLevel.LOW,
				EvidenceConfidenceReason.CONTRADICTORY_EVIDENCE,
				EvidenceConfidenceScope.EVIDENCE,
				trustScore(
						EvidenceTrustScoreLevel.LOW,
						EvidenceTrustScoreReason.CONTRADICTORY_EVIDENCE,
						EvidenceTrustScoreScope.EVIDENCE,
						governancePolicy(
								EvidenceTrustLevel.TRUSTED,
								EvidenceIntegrityStatus.CONTRADICTORY,
								EvidenceClassification.PUBLIC_SAFE,
								provenance(true, false, false)
						),
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
				)
		));

		assertThat(result.reason())
				.isEqualTo(EvidenceConfidenceIntegrationReason.CONTRADICTORY_CONFIDENCE);
		assertThat(result.apiResponse().summary().riskLevel())
				.isEqualTo(OperationalUncertainty.HIGH);
		assertThat(result.apiResponse().summary().uncertaintyDetected()).isTrue();
	}

	@Test
	void shouldRemainNonRecommendationAndNonExecutionAuthority() {
		EvidenceConfidenceIntegrationResult result = integration.integrate(confidence(
				EvidenceConfidenceLevel.HIGH,
				EvidenceConfidenceReason.CORROBORATING_EVIDENCE,
				EvidenceConfidenceScope.EVIDENCE,
				trustScore(
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
				)
		));

		assertThat(result.recommendationAuthority()).isFalse();
		assertThat(result.executionAuthority()).isFalse();
		assertThat(result.readOnly()).isTrue();
	}

	@Test
	void shouldRejectNullConfidence() {
		assertThatThrownBy(() -> integration.integrate(null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("confidence must not be null");
	}

	private EvidenceConfidence confidence(
			EvidenceConfidenceLevel level,
			EvidenceConfidenceReason reason,
			EvidenceConfidenceScope scope,
			EvidenceTrustScore trustScore
	) {
		return new EvidenceConfidence(level, reason, scope, trustScore);
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
						EvidenceLineageIntegrationStatus.INTEGRATED,
						EvidenceLineageIntegrationReason.UNKNOWN,
						EvidenceLineageIntegrationScope.API_EXPOSURE
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
