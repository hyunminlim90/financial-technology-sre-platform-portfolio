package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class OperationalReliabilityEvidenceReliabilityIntegrationTest {

	private final EvidenceReliabilityIntegration integration =
			new EvidenceReliabilityIntegration();

	@Test
	void shouldRemainReadOnlyAndNonMutating() {
		assertThat(integration.readOnly()).isTrue();
		assertThat(integration.mutatesEvidence()).isFalse();
		assertThat(integration.recommendationAuthority()).isFalse();
		assertThat(integration.executionAuthority()).isFalse();
		assertThat(integration.mutatesPortfolioKnowledgeSource()).isFalse();
	}

	@Test
	void shouldHideBlockedReliabilityFromApiResponse() {
		EvidenceReliabilityIntegrationResult result = integration.integrate(reliability(
				EvidenceReliabilityLevel.BLOCKED,
				EvidenceReliabilityReason.GOVERNANCE_BLOCKED,
				false,
				confidence(
						EvidenceConfidenceLevel.LOW,
						EvidenceConfidenceReason.PARTIAL_EVIDENCE,
						EvidenceConfidenceScope.API_BOUNDARY,
						trustScore(
								EvidenceTrustScoreLevel.UNTRUSTED,
								EvidenceTrustScoreReason.BLOCKED_EVIDENCE,
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
										)
								)
						)
				)
		));

		assertThat(result.status()).isEqualTo(EvidenceReliabilityIntegrationStatus.BLOCKED);
		assertThat(result.apiResponse().status()).isEqualTo(EvidenceRuntimeApiStatus.REJECTED);
	}

	@Test
	void shouldBlockAssessmentCertaintyForUnreliableEvidence() {
		EvidenceReliabilityIntegrationResult result = integration.integrate(reliability(
				EvidenceReliabilityLevel.UNRELIABLE,
				EvidenceReliabilityReason.UNTRUSTED_EVIDENCE,
				false,
				confidence(
						EvidenceConfidenceLevel.MEDIUM,
						EvidenceConfidenceReason.PARTIAL_EVIDENCE,
						EvidenceConfidenceScope.ASSESSMENT,
						trustScore(
								EvidenceTrustScoreLevel.UNTRUSTED,
								EvidenceTrustScoreReason.MISSING_PROVENANCE,
								governancePolicy(
										EvidenceTrustLevel.UNTRUSTED,
										EvidenceIntegrityStatus.MISSING,
										EvidenceClassification.UNKNOWN,
										EvidenceProvenance.missingProvenance()
								),
								lineage(
										EvidenceLineageStatus.PARTIAL,
										EvidenceLineageReason.MISSING_PROVENANCE,
										governancePolicy(
												EvidenceTrustLevel.UNTRUSTED,
												EvidenceIntegrityStatus.MISSING,
												EvidenceClassification.UNKNOWN,
												EvidenceProvenance.missingProvenance()
										)
								)
						)
				)
		));

		assertThat(result.status()).isEqualTo(EvidenceReliabilityIntegrationStatus.UNRELIABLE);
		assertThat(result.scope())
				.isEqualTo(EvidenceReliabilityIntegrationScope.ASSESSMENT_UNCERTAINTY);
		assertThat(result.apiResponse().status()).isEqualTo(EvidenceRuntimeApiStatus.UNTRUSTED);
	}

	@Test
	void shouldRequireOperatorWarningForLowReliability() {
		EvidenceReliabilityIntegrationResult result = integration.integrate(reliability(
				EvidenceReliabilityLevel.LOW,
				EvidenceReliabilityReason.LOW_CONFIDENCE,
				false,
				confidence(
						EvidenceConfidenceLevel.LOW,
						EvidenceConfidenceReason.CONTRADICTORY_EVIDENCE,
						EvidenceConfidenceScope.OPERATOR_VIEW,
						trustScore(
								EvidenceTrustScoreLevel.LOW,
								EvidenceTrustScoreReason.CONTRADICTORY_EVIDENCE,
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
										)
								)
						)
				)
		));

		assertThat(result.status()).isEqualTo(EvidenceReliabilityIntegrationStatus.WARNING);
		assertThat(result.reason()).isEqualTo(EvidenceReliabilityIntegrationReason.LOW_RELIABILITY);
		assertThat(result.scope())
				.isEqualTo(EvidenceReliabilityIntegrationScope.OPERATOR_WARNING_VIEW);
	}

	@Test
	void shouldMarkMediumReliabilityAsPartialReliability() {
		EvidenceReliabilityIntegrationResult result = integration.integrate(reliability(
				EvidenceReliabilityLevel.MEDIUM,
				EvidenceReliabilityReason.UNKNOWN,
				false,
				confidence(
						EvidenceConfidenceLevel.MEDIUM,
						EvidenceConfidenceReason.PARTIAL_EVIDENCE,
						EvidenceConfidenceScope.OPERATOR_VIEW,
						trustScore(
								EvidenceTrustScoreLevel.MEDIUM,
								EvidenceTrustScoreReason.PARTIAL_PROVENANCE,
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
										)
								)
						)
				)
		));

		assertThat(result.status())
				.isEqualTo(EvidenceReliabilityIntegrationStatus.PARTIAL_RELIABILITY);
		assertThat(result.reason())
				.isEqualTo(EvidenceReliabilityIntegrationReason.MEDIUM_RELIABILITY);
	}

	@Test
	void shouldAllowOnlyHighReliabilityAsTrustedEvidenceViewCandidate() {
		EvidenceReliabilityIntegrationResult result = integration.integrate(reliability(
				EvidenceReliabilityLevel.HIGH,
				EvidenceReliabilityReason.HIGH_RELIABILITY_EVIDENCE,
				false,
				confidence(
						EvidenceConfidenceLevel.HIGH,
						EvidenceConfidenceReason.CORROBORATING_EVIDENCE,
						EvidenceConfidenceScope.EVIDENCE,
						trustScore(
								EvidenceTrustScoreLevel.HIGH,
								EvidenceTrustScoreReason.TRUSTED_PROVENANCE,
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
										)
								)
						)
				)
		));

		assertThat(result.status()).isEqualTo(EvidenceReliabilityIntegrationStatus.TRUSTED);
		assertThat(result.reason()).isEqualTo(EvidenceReliabilityIntegrationReason.HIGH_RELIABILITY);
		assertThat(result.scope())
				.isEqualTo(EvidenceReliabilityIntegrationScope.TRUSTED_EVIDENCE_VIEW);
	}

	@Test
	void shouldPropagatePaymentSafetyUncertaintyToRisk() {
		EvidenceReliabilityIntegrationResult result = integration.integrate(reliability(
				EvidenceReliabilityLevel.RESTRICTED,
				EvidenceReliabilityReason.PAYMENT_SAFETY_UNCERTAINTY,
				true,
				confidence(
						EvidenceConfidenceLevel.INSUFFICIENT,
						EvidenceConfidenceReason.PAYMENT_EVIDENCE_MISSING,
						EvidenceConfidenceScope.PAYMENT_EVIDENCE,
						trustScore(
								EvidenceTrustScoreLevel.MEDIUM,
								EvidenceTrustScoreReason.PAYMENT_RESTRICTED_EVIDENCE,
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
										)
								)
						)
				)
		));

		assertThat(result.status()).isEqualTo(EvidenceReliabilityIntegrationStatus.RESTRICTED);
		assertThat(result.reason())
				.isEqualTo(EvidenceReliabilityIntegrationReason.PAYMENT_SAFETY_UNCERTAINTY);
		assertThat(result.scope())
				.isEqualTo(EvidenceReliabilityIntegrationScope.PAYMENT_RISK_VIEW);
		assertThat(result.apiResponse().summary().riskLevel()).isEqualTo(OperationalUncertainty.HIGH);
		assertThat(result.apiResponse().summary().paymentSafetyState())
				.isEqualTo(OperationalUncertainty.HIGH);
	}

	@Test
	void shouldRemainNonRecommendationAndNonExecutionAuthority() {
		EvidenceReliabilityIntegrationResult result = integration.integrate(reliability(
				EvidenceReliabilityLevel.HIGH,
				EvidenceReliabilityReason.HIGH_RELIABILITY_EVIDENCE,
				false,
				confidence(
						EvidenceConfidenceLevel.HIGH,
						EvidenceConfidenceReason.CORROBORATING_EVIDENCE,
						EvidenceConfidenceScope.EVIDENCE,
						trustScore(
								EvidenceTrustScoreLevel.HIGH,
								EvidenceTrustScoreReason.TRUSTED_PROVENANCE,
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
										)
								)
						)
				)
		));

		assertThat(result.readOnly()).isTrue();
		assertThat(result.recommendationAuthority()).isFalse();
		assertThat(result.executionAuthority()).isFalse();
	}

	@Test
	void shouldRejectNullReliability() {
		assertThatThrownBy(() -> integration.integrate(null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("reliability must not be null");
	}

	private EvidenceReliability reliability(
			EvidenceReliabilityLevel level,
			EvidenceReliabilityReason reason,
			boolean paymentSafetyUncertainty,
			EvidenceConfidence confidence
	) {
		return new EvidenceReliability(
				level,
				reason,
				confidence.scope() == EvidenceConfidenceScope.PAYMENT_EVIDENCE
						? EvidenceReliabilityScope.PAYMENT_EVIDENCE
						: EvidenceReliabilityScope.EVIDENCE,
				confidence.trustScore().governanceIntegrationResult().governancePolicy(),
				confidence.trustScore().lineageIntegrationResult().lineage(),
				confidence.trustScore(),
				confidence,
				confidence.level() != EvidenceConfidenceLevel.INSUFFICIENT,
				paymentSafetyUncertainty
		);
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
			EvidenceGovernancePolicy policy,
			EvidenceLineage lineage
	) {
		EvidenceRuntimeApiResponse apiResponse = apiResponse();
		return new EvidenceTrustScore(
				level,
				reason,
				policy.classification() == EvidenceClassification.RESTRICTED
						? EvidenceTrustScoreScope.PAYMENT_EVIDENCE
						: EvidenceTrustScoreScope.EVIDENCE,
				new EvidenceGovernanceIntegrationResult(
						policy,
						apiResponse,
						EvidenceGovernanceIntegrationStatus.INTEGRATED,
						EvidenceGovernanceIntegrationReason.UNKNOWN,
						EvidenceGovernanceIntegrationScope.API_EXPOSURE
				),
				new EvidenceLineageIntegrationResult(
						lineage,
						apiResponse,
						EvidenceLineageIntegrationStatus.INTEGRATED,
						EvidenceLineageIntegrationReason.UNKNOWN,
						EvidenceLineageIntegrationScope.API_EXPOSURE
				)
		);
	}

	private EvidenceLineage lineage(
			EvidenceLineageStatus status,
			EvidenceLineageReason reason,
			EvidenceGovernancePolicy governancePolicy
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
						new EvidenceLineageEdge(EvidenceLineageNode.ADAPTER, EvidenceLineageNode.ROUTING)
				),
				status,
				reason,
				governancePolicy,
				status == EvidenceLineageStatus.BLOCKED
						? OperationalUncertainty.CRITICAL
						: status == EvidenceLineageStatus.RESTRICTED
						? OperationalUncertainty.HIGH
						: OperationalUncertainty.LOW
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

	private EvidenceRuntimeApiResponse apiResponse() {
		return new EvidenceRuntimeApiResponse(
				new EvidenceRuntimeSummaryView(
						EvidenceRuntimeSummaryStatus.HEALTHY,
						OperationalUncertainty.LOW,
						OperationalUncertainty.LOW,
						false,
						EvidenceRuntimeSummaryReason.UNKNOWN,
						true,
						EvidenceCompleteness.COMPLETE
				),
				EvidenceRuntimeApiStatus.READABLE,
				EvidenceRuntimeApiRejectionReason.UNKNOWN
		);
	}
}
