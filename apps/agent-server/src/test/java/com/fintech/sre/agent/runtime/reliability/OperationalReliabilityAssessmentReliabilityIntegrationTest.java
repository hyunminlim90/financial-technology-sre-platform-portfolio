package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class OperationalReliabilityAssessmentReliabilityIntegrationTest {

	private final AssessmentReliabilityIntegration integration =
			new AssessmentReliabilityIntegration();

	@Test
	void shouldRemainReadOnlyAndNonMutating() {
		assertThat(integration.readOnly()).isTrue();
		assertThat(integration.mutatesAssessment()).isFalse();
		assertThat(integration.recommendationAuthority()).isFalse();
		assertThat(integration.executionAuthority()).isFalse();
		assertThat(integration.mutatesPortfolioKnowledgeSource()).isFalse();
	}

	@Test
	void shouldPreventLifecycleStableForBlockedAssessmentReliability() {
		AssessmentReliabilityIntegrationResult result = integration.integrate(
				assessmentReliability(
						AssessmentReliabilityLevel.BLOCKED,
						AssessmentReliabilityReason.BLOCKED_EVIDENCE,
						false,
						evidenceReliability(
								EvidenceReliabilityLevel.BLOCKED,
								EvidenceReliabilityReason.GOVERNANCE_BLOCKED,
								false,
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
						)
				)
		);

		assertThat(result.status()).isEqualTo(AssessmentReliabilityIntegrationStatus.BLOCKED);
		assertThat(result.lifecycleStableAllowed()).isFalse();
	}

	@Test
	void shouldPreventRecommendationCertaintyForUnreliableAssessmentReliability() {
		AssessmentReliabilityIntegrationResult result = integration.integrate(
				assessmentReliability(
						AssessmentReliabilityLevel.UNRELIABLE,
						AssessmentReliabilityReason.UNRELIABLE_EVIDENCE,
						false,
						evidenceReliability(
								EvidenceReliabilityLevel.UNRELIABLE,
								EvidenceReliabilityReason.UNTRUSTED_EVIDENCE,
								false,
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
						)
				)
		);

		assertThat(result.status()).isEqualTo(AssessmentReliabilityIntegrationStatus.UNRELIABLE);
		assertThat(result.recommendationCertaintyAllowed()).isFalse();
	}

	@Test
	void shouldRequireOperatorFacingWarningForLowAssessmentReliability() {
		AssessmentReliabilityIntegrationResult result = integration.integrate(
				assessmentReliability(
						AssessmentReliabilityLevel.LOW,
						AssessmentReliabilityReason.LOW_EVIDENCE_RELIABILITY,
						false,
						evidenceReliability(
								EvidenceReliabilityLevel.LOW,
								EvidenceReliabilityReason.LOW_CONFIDENCE,
								false,
								true,
								confidence(
										EvidenceConfidenceLevel.LOW,
										EvidenceConfidenceReason.PARTIAL_EVIDENCE,
										EvidenceConfidenceScope.OPERATOR_VIEW,
										trustScore(
												EvidenceTrustScoreLevel.LOW,
												EvidenceTrustScoreReason.DEGRADED_INTEGRITY,
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
														)
												)
										)
								)
						)
				)
		);

		assertThat(result.status()).isEqualTo(AssessmentReliabilityIntegrationStatus.WARNING);
		assertThat(result.reason())
				.isEqualTo(AssessmentReliabilityIntegrationReason.LOW_ASSESSMENT_RELIABILITY);
		assertThat(result.scope())
				.isEqualTo(AssessmentReliabilityIntegrationScope.OPERATOR_WARNING_VIEW);
	}

	@Test
	void shouldMarkMediumAssessmentReliabilityAsPartial() {
		AssessmentReliabilityIntegrationResult result = integration.integrate(
				assessmentReliability(
						AssessmentReliabilityLevel.MEDIUM,
						AssessmentReliabilityReason.UNKNOWN,
						true,
						evidenceReliability(
								EvidenceReliabilityLevel.MEDIUM,
								EvidenceReliabilityReason.UNKNOWN,
								false,
								true,
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
						)
				)
		);

		assertThat(result.status())
				.isEqualTo(AssessmentReliabilityIntegrationStatus.PARTIAL_ASSESSMENT_RELIABILITY);
		assertThat(result.reason())
				.isEqualTo(AssessmentReliabilityIntegrationReason.MEDIUM_ASSESSMENT_RELIABILITY);
	}

	@Test
	void shouldAllowOnlyHighAssessmentReliabilityAsReliableAssessmentViewCandidate() {
		AssessmentReliabilityIntegrationResult result = integration.integrate(
				assessmentReliability(
						AssessmentReliabilityLevel.HIGH,
						AssessmentReliabilityReason.HIGH_EVIDENCE_RELIABILITY,
						true,
						evidenceReliability(
								EvidenceReliabilityLevel.HIGH,
								EvidenceReliabilityReason.HIGH_RELIABILITY_EVIDENCE,
								false,
								true,
								confidence(
										EvidenceConfidenceLevel.HIGH,
										EvidenceConfidenceReason.CORROBORATING_EVIDENCE,
										EvidenceConfidenceScope.OBSERVABLE_RUNTIME,
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
						)
				)
		);

		assertThat(result.status()).isEqualTo(AssessmentReliabilityIntegrationStatus.RELIABLE);
		assertThat(result.reason())
				.isEqualTo(AssessmentReliabilityIntegrationReason.HIGH_ASSESSMENT_RELIABILITY);
		assertThat(result.scope())
				.isEqualTo(AssessmentReliabilityIntegrationScope.RELIABLE_ASSESSMENT_VIEW);
		assertThat(result.lifecycleStableAllowed()).isTrue();
		assertThat(result.recommendationCertaintyAllowed()).isTrue();
	}

	@Test
	void shouldPropagatePaymentSafetyUncertaintyToLifecycleRisk() {
		AssessmentReliabilityIntegrationResult result = integration.integrate(
				assessmentReliability(
						AssessmentReliabilityLevel.LOW,
						AssessmentReliabilityReason.PAYMENT_SAFETY_UNCERTAINTY,
						false,
						evidenceReliability(
								EvidenceReliabilityLevel.RESTRICTED,
								EvidenceReliabilityReason.PAYMENT_SAFETY_UNCERTAINTY,
								true,
								false,
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
						)
				)
		);

		assertThat(result.reason())
				.isEqualTo(AssessmentReliabilityIntegrationReason.PAYMENT_SAFETY_UNCERTAINTY);
		assertThat(result.scope())
				.isEqualTo(AssessmentReliabilityIntegrationScope.PAYMENT_RISK_VIEW);
		assertThat(result.apiResponse().summary().riskLevel())
				.isEqualTo(OperationalUncertainty.HIGH);
	}

	@Test
	void shouldPropagateContradictoryAssessmentToLifecycleUncertainty() {
		AssessmentReliabilityIntegrationResult result = integration.integrate(
				assessmentReliability(
						AssessmentReliabilityLevel.LOW,
						AssessmentReliabilityReason.CONTRADICTORY_EVIDENCE,
						false,
						evidenceReliability(
								EvidenceReliabilityLevel.LOW,
								EvidenceReliabilityReason.CONTRADICTORY_EVIDENCE,
								false,
								true,
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
						)
				)
		);

		assertThat(result.reason())
				.isEqualTo(AssessmentReliabilityIntegrationReason.CONTRADICTORY_ASSESSMENT);
		assertThat(result.apiResponse().summary().uncertaintyDetected()).isTrue();
	}

	@Test
	void shouldRemainNonRecommendationAndNonExecutionAuthority() {
		AssessmentReliabilityIntegrationResult result = integration.integrate(
				assessmentReliability(
						AssessmentReliabilityLevel.HIGH,
						AssessmentReliabilityReason.HIGH_EVIDENCE_RELIABILITY,
						true,
						evidenceReliability(
								EvidenceReliabilityLevel.HIGH,
								EvidenceReliabilityReason.HIGH_RELIABILITY_EVIDENCE,
								false,
								true,
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
						)
				)
		);

		assertThat(result.readOnly()).isTrue();
		assertThat(result.recommendation()).isFalse();
		assertThat(result.executionPermission()).isFalse();
		assertThat(result.actionAdmission()).isFalse();
	}

	@Test
	void shouldRejectNullAssessmentReliability() {
		assertThatThrownBy(() -> integration.integrate(null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("assessmentReliability must not be null");
	}

	private AssessmentReliability assessmentReliability(
			AssessmentReliabilityLevel level,
			AssessmentReliabilityReason reason,
			boolean assessmentCertaintyAllowed,
			EvidenceReliability evidenceReliability
	) {
		return new AssessmentReliability(
				level,
				reason,
				evidenceReliability.scope() == EvidenceReliabilityScope.PAYMENT_EVIDENCE
						? AssessmentReliabilityScope.PAYMENT_EVIDENCE
						: AssessmentReliabilityScope.ASSESSMENT,
				evidenceReliability,
				assessmentCertaintyAllowed
		);
	}

	private EvidenceReliability evidenceReliability(
			EvidenceReliabilityLevel level,
			EvidenceReliabilityReason reason,
			boolean paymentSafetyUncertainty,
			boolean assessmentCertaintyAllowed,
			EvidenceConfidence confidence
	) {
		return new EvidenceReliability(
				level,
				reason,
				confidence.scope() == EvidenceConfidenceScope.PAYMENT_EVIDENCE
						? EvidenceReliabilityScope.PAYMENT_EVIDENCE
						: EvidenceReliabilityScope.ASSESSMENT,
				confidence.trustScore().governanceIntegrationResult().governancePolicy(),
				confidence.trustScore().lineageIntegrationResult().lineage(),
				confidence.trustScore(),
				confidence,
				assessmentCertaintyAllowed,
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
			EvidenceGovernancePolicy governancePolicy,
			EvidenceLineage lineage
	) {
		EvidenceRuntimeApiResponse apiResponse = apiResponse();
		return new EvidenceTrustScore(
				level,
				reason,
				governancePolicy.classification() == EvidenceClassification.RESTRICTED
						? EvidenceTrustScoreScope.PAYMENT_EVIDENCE
						: EvidenceTrustScoreScope.EVIDENCE,
				new EvidenceGovernanceIntegrationResult(
						governancePolicy,
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
				Instant.parse("2026-06-03T00:00:00Z"),
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
