package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class OperationalReliabilityEvidenceConfidenceTest {

	private final EvidenceConfidenceCalculator calculator =
			new EvidenceConfidenceCalculator();

	@Test
	void shouldRemainReadOnlyAndNonAuthoritative() {
		EvidenceConfidence confidence = calculator.calculate(trustScore(
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
						OperationalUncertainty.LOW,
						true,
						true
				)
		));

		assertThat(confidence.readOnly()).isTrue();
		assertThat(confidence.recommendation()).isFalse();
		assertThat(confidence.executionPermission()).isFalse();
		assertThat(confidence.actionAdmissionResult()).isFalse();
		assertThat(confidence.mutatesPortfolioKnowledgeSource()).isFalse();
	}

	@Test
	void shouldDowngradeHighTrustWithInsufficientEvidence() {
		EvidenceGovernancePolicy policy = governancePolicy(
				EvidenceTrustLevel.TRUSTED,
				EvidenceIntegrityStatus.INTACT,
				EvidenceClassification.PUBLIC_SAFE,
				provenance(true, false, false)
		);
		EvidenceConfidence confidence = calculator.calculate(trustScore(
				EvidenceTrustScoreLevel.HIGH,
				EvidenceTrustScoreReason.TRUSTED_PROVENANCE,
				EvidenceTrustScoreScope.EVIDENCE,
				policy,
				lineage(
						EvidenceLineageStatus.INCOMPLETE,
						EvidenceLineageReason.MISSING_COLLECTION_STAGE,
						policy,
						OperationalUncertainty.HIGH,
						false,
						true
				)
		));

		assertThat(confidence.level()).isEqualTo(EvidenceConfidenceLevel.INSUFFICIENT);
		assertThat(confidence.reason()).isEqualTo(EvidenceConfidenceReason.INSUFFICIENT_EVIDENCE);
	}

	@Test
	void shouldReturnHighForCorroboratingEvidence() {
		EvidenceGovernancePolicy policy = governancePolicy(
				EvidenceTrustLevel.TRUSTED,
				EvidenceIntegrityStatus.INTACT,
				EvidenceClassification.PUBLIC_SAFE,
				provenance(true, false, false)
		);
		EvidenceConfidence confidence = calculator.calculate(trustScore(
				EvidenceTrustScoreLevel.HIGH,
				EvidenceTrustScoreReason.TRUSTED_PROVENANCE,
				EvidenceTrustScoreScope.EVIDENCE,
				policy,
				lineage(
						EvidenceLineageStatus.COMPLETE,
						EvidenceLineageReason.UNKNOWN,
						policy,
						OperationalUncertainty.LOW,
						true,
						true
				)
		));

		assertThat(confidence.level()).isEqualTo(EvidenceConfidenceLevel.HIGH);
		assertThat(confidence.reason()).isEqualTo(EvidenceConfidenceReason.CORROBORATING_EVIDENCE);
	}

	@Test
	void shouldDowngradeForContradictoryEvidence() {
		EvidenceGovernancePolicy policy = governancePolicy(
				EvidenceTrustLevel.TRUSTED,
				EvidenceIntegrityStatus.CONTRADICTORY,
				EvidenceClassification.PUBLIC_SAFE,
				provenance(true, false, false)
		);
		EvidenceConfidence confidence = calculator.calculate(trustScore(
				EvidenceTrustScoreLevel.LOW,
				EvidenceTrustScoreReason.CONTRADICTORY_EVIDENCE,
				EvidenceTrustScoreScope.EVIDENCE,
				policy,
				lineage(
						EvidenceLineageStatus.PARTIAL,
						EvidenceLineageReason.CONTRADICTORY_EVIDENCE,
						policy,
						OperationalUncertainty.HIGH,
						true,
						true
				)
		));

		assertThat(confidence.level()).isEqualTo(EvidenceConfidenceLevel.LOW);
		assertThat(confidence.reason()).isEqualTo(EvidenceConfidenceReason.CONTRADICTORY_EVIDENCE);
	}

	@Test
	void shouldDowngradeWhenPaymentEvidenceIsMissing() {
		EvidenceGovernancePolicy policy = governancePolicy(
				EvidenceTrustLevel.TRUSTED,
				EvidenceIntegrityStatus.INTACT,
				EvidenceClassification.RESTRICTED,
				EvidenceProvenance.missingProvenance()
		);
		EvidenceConfidence confidence = calculator.calculate(trustScore(
				EvidenceTrustScoreLevel.MEDIUM,
				EvidenceTrustScoreReason.PAYMENT_RESTRICTED_EVIDENCE,
				EvidenceTrustScoreScope.PAYMENT_EVIDENCE,
				policy,
				lineage(
						EvidenceLineageStatus.INCOMPLETE,
						EvidenceLineageReason.MISSING_PROVENANCE,
						policy,
						OperationalUncertainty.HIGH,
						false,
						true
				)
		));

		assertThat(confidence.level()).isEqualTo(EvidenceConfidenceLevel.INSUFFICIENT);
		assertThat(confidence.reason()).isEqualTo(EvidenceConfidenceReason.PAYMENT_EVIDENCE_MISSING);
		assertThat(confidence.scope()).isEqualTo(EvidenceConfidenceScope.PAYMENT_EVIDENCE);
	}

	@Test
	void shouldDowngradeForUnknownEvidence() {
		EvidenceGovernancePolicy policy = governancePolicy(
				EvidenceTrustLevel.UNKNOWN,
				EvidenceIntegrityStatus.UNKNOWN,
				EvidenceClassification.UNKNOWN,
				provenance(true, false, false)
		);
		EvidenceConfidence confidence = calculator.calculate(trustScore(
				EvidenceTrustScoreLevel.UNKNOWN,
				EvidenceTrustScoreReason.UNKNOWN,
				EvidenceTrustScoreScope.EVIDENCE,
				policy,
				lineage(
						EvidenceLineageStatus.UNKNOWN,
						EvidenceLineageReason.UNKNOWN,
						policy,
						OperationalUncertainty.MODERATE,
						true,
						true
				)
		));

		assertThat(confidence.level()).isEqualTo(EvidenceConfidenceLevel.LOW);
		assertThat(confidence.reason()).isEqualTo(EvidenceConfidenceReason.UNKNOWN_EVIDENCE);
	}

	@Test
	void shouldNotExposeRawPayloadVendorOrCredentials() {
		EvidenceConfidence confidence = calculator.calculate(trustScore(
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
						OperationalUncertainty.LOW,
						true,
						true
				)
		));

		assertThat(confidence.exposesRawPayload()).isFalse();
		assertThat(confidence.exposesVendorDetail()).isFalse();
		assertThat(confidence.exposesCredentialConfiguration()).isFalse();
	}

	@Test
	void shouldRemainDeterministicAndNonNumeric() {
		assertThat(calculator.deterministicRuleBased()).isTrue();
		assertThat(calculator.numericScore()).isFalse();
		assertThat(calculator.weightingAlgorithm()).isFalse();
		assertThat(calculator.mlInference()).isFalse();
		assertThat(calculator.bayesianInference()).isFalse();
		assertThat(calculator.statisticalConfidence()).isFalse();
	}

	@Test
	void shouldRejectNullTrustScore() {
		assertThatThrownBy(() -> calculator.calculate(null))
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
			OperationalUncertainty risk,
			boolean collectionPresent,
			boolean assessmentPresent
	) {
		List<EvidenceLineageNode> nodes = new java.util.ArrayList<>();
		nodes.add(EvidenceLineageNode.SOURCE);
		nodes.add(EvidenceLineageNode.ADAPTER);
		nodes.add(EvidenceLineageNode.ROUTING);
		nodes.add(EvidenceLineageNode.DISPATCH);
		nodes.add(EvidenceLineageNode.EXECUTION);
		if (collectionPresent) {
			nodes.add(EvidenceLineageNode.COLLECTION);
		}
		if (assessmentPresent) {
			nodes.add(EvidenceLineageNode.ASSESSMENT);
		}
		nodes.add(EvidenceLineageNode.SUMMARY);

		List<EvidenceLineageEdge> edges = new java.util.ArrayList<>();
		for (int i = 0; i < nodes.size() - 1; i++) {
			edges.add(new EvidenceLineageEdge(nodes.get(i), nodes.get(i + 1)));
		}

		return new EvidenceLineage(
				nodes,
				edges,
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
