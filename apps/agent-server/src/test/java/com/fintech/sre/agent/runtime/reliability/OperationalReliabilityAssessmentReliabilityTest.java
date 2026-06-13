package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class OperationalReliabilityAssessmentReliabilityTest {

	private final AssessmentReliabilityEvaluator evaluator =
			new AssessmentReliabilityEvaluator();

	@Test
	void shouldRemainReadOnlyAndNonAuthoritative() {
		AssessmentReliability reliability = evaluator.evaluate(evidenceReliability(
				EvidenceReliabilityLevel.HIGH,
				EvidenceReliabilityReason.HIGH_RELIABILITY_EVIDENCE,
				false,
				true,
				confidence(
						EvidenceConfidenceLevel.HIGH,
						EvidenceConfidenceReason.CORROBORATING_EVIDENCE,
						EvidenceConfidenceScope.ASSESSMENT,
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

		assertThat(reliability.readOnly()).isTrue();
		assertThat(reliability.recommendation()).isFalse();
		assertThat(reliability.executionPermission()).isFalse();
		assertThat(reliability.actionAdmission()).isFalse();
		assertThat(reliability.exposesRawPayload()).isFalse();
		assertThat(reliability.exposesVendorDetail()).isFalse();
		assertThat(reliability.exposesCredentialConfiguration()).isFalse();
		assertThat(reliability.mutatesPortfolioKnowledgeSource()).isFalse();
	}

	@Test
	void shouldBlockAssessmentReliabilityForBlockedEvidence() {
		AssessmentReliability reliability = evaluator.evaluate(evidenceReliability(
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
		));

		assertThat(reliability.level()).isEqualTo(AssessmentReliabilityLevel.BLOCKED);
		assertThat(reliability.reason()).isEqualTo(AssessmentReliabilityReason.BLOCKED_EVIDENCE);
		assertThat(reliability.assessmentCertaintyAllowed()).isFalse();
	}

	@Test
	void shouldMarkUnreliableEvidenceAsUnreliableAssessment() {
		AssessmentReliability reliability = evaluator.evaluate(evidenceReliability(
				EvidenceReliabilityLevel.UNRELIABLE,
				EvidenceReliabilityReason.UNTRUSTED_EVIDENCE,
				false,
				true,
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

		assertThat(reliability.level()).isEqualTo(AssessmentReliabilityLevel.UNRELIABLE);
		assertThat(reliability.reason()).isEqualTo(AssessmentReliabilityReason.UNRELIABLE_EVIDENCE);
		assertThat(reliability.assessmentCertaintyAllowed()).isFalse();
	}

	@Test
	void shouldDowngradeLowEvidenceReliability() {
		AssessmentReliability reliability = evaluator.evaluate(evidenceReliability(
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
		));

		assertThat(reliability.level()).isEqualTo(AssessmentReliabilityLevel.LOW);
		assertThat(reliability.reason())
				.isEqualTo(AssessmentReliabilityReason.LOW_EVIDENCE_RELIABILITY);
	}

	@Test
	void shouldDisallowAssessmentCertaintyForInsufficientConfidence() {
		AssessmentReliability reliability = evaluator.evaluate(evidenceReliability(
				EvidenceReliabilityLevel.LOW,
				EvidenceReliabilityReason.INSUFFICIENT_CONFIDENCE,
				false,
				false,
				confidence(
						EvidenceConfidenceLevel.INSUFFICIENT,
						EvidenceConfidenceReason.INSUFFICIENT_EVIDENCE,
						EvidenceConfidenceScope.ASSESSMENT,
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

		assertThat(reliability.level()).isEqualTo(AssessmentReliabilityLevel.LOW);
		assertThat(reliability.reason())
				.isEqualTo(AssessmentReliabilityReason.INSUFFICIENT_CONFIDENCE);
		assertThat(reliability.assessmentCertaintyAllowed()).isFalse();
	}

	@Test
	void shouldDowngradePaymentSafetyUncertainty() {
		AssessmentReliability reliability = evaluator.evaluate(evidenceReliability(
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
		));

		assertThat(reliability.level()).isEqualTo(AssessmentReliabilityLevel.LOW);
		assertThat(reliability.reason())
				.isEqualTo(AssessmentReliabilityReason.PAYMENT_SAFETY_UNCERTAINTY);
		assertThat(reliability.assessmentCertaintyAllowed()).isFalse();
	}

	@Test
	void shouldDowngradeContradictoryEvidence() {
		AssessmentReliability reliability = evaluator.evaluate(evidenceReliability(
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
		));

		assertThat(reliability.level()).isEqualTo(AssessmentReliabilityLevel.LOW);
		assertThat(reliability.reason())
				.isEqualTo(AssessmentReliabilityReason.CONTRADICTORY_EVIDENCE);
	}

	@Test
	void shouldRequireHighEvidenceReliabilityForHighAssessmentReliability() {
		AssessmentReliability reliability = evaluator.evaluate(evidenceReliability(
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
		));

		assertThat(reliability.level()).isEqualTo(AssessmentReliabilityLevel.HIGH);
		assertThat(reliability.reason())
				.isEqualTo(AssessmentReliabilityReason.HIGH_EVIDENCE_RELIABILITY);
		assertThat(reliability.assessmentCertaintyAllowed()).isTrue();
	}

	@Test
	void shouldRejectNullEvidenceReliability() {
		assertThatThrownBy(() -> evaluator.evaluate(null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("evidenceReliability must not be null");
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
