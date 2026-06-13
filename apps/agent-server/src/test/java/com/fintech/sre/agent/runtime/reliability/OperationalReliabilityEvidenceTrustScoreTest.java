package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class OperationalReliabilityEvidenceTrustScoreTest {

	private final EvidenceTrustScoreCalculator calculator =
			new EvidenceTrustScoreCalculator();

	@Test
	void shouldRemainReadOnlyAndNonAuthoritative() {
		EvidenceTrustScore score = calculator.calculate(
				governanceIntegrationResult(
						governancePolicy(
								EvidenceTrustLevel.TRUSTED,
								EvidenceIntegrityStatus.INTACT,
								EvidenceClassification.PUBLIC_SAFE,
								provenance(true, false, false)
						)
				),
				lineageIntegrationResult(
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

		assertThat(score.readOnly()).isTrue();
		assertThat(score.recommendation()).isFalse();
		assertThat(score.executionPermission()).isFalse();
		assertThat(score.actionAdmissionResult()).isFalse();
		assertThat(score.mutatesPortfolioKnowledgeSource()).isFalse();
	}

	@Test
	void shouldReturnHighForTrustedProvenanceCompleteLineageAndIntactIntegrity() {
		EvidenceTrustScore score = calculator.calculate(
				governanceIntegrationResult(
						governancePolicy(
								EvidenceTrustLevel.TRUSTED,
								EvidenceIntegrityStatus.INTACT,
								EvidenceClassification.PUBLIC_SAFE,
								provenance(true, false, false)
						)
				),
				lineageIntegrationResult(
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

		assertThat(score.level()).isEqualTo(EvidenceTrustScoreLevel.HIGH);
		assertThat(score.reason()).isEqualTo(EvidenceTrustScoreReason.TRUSTED_PROVENANCE);
	}

	@Test
	void shouldReturnMediumForPartiallyTrustedProvenance() {
		EvidenceTrustScore score = calculator.calculate(
				governanceIntegrationResult(
						governancePolicy(
								EvidenceTrustLevel.PARTIALLY_TRUSTED,
								EvidenceIntegrityStatus.INTACT,
								EvidenceClassification.INTERNAL,
								provenance(false, false, false)
						)
				),
				lineageIntegrationResult(
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
		);

		assertThat(score.level()).isEqualTo(EvidenceTrustScoreLevel.MEDIUM);
		assertThat(score.reason()).isEqualTo(EvidenceTrustScoreReason.PARTIAL_PROVENANCE);
	}

	@Test
	void shouldDowngradeForMissingProvenance() {
		EvidenceGovernancePolicy policy = governancePolicy(
				EvidenceTrustLevel.UNTRUSTED,
				EvidenceIntegrityStatus.MISSING,
				EvidenceClassification.UNKNOWN,
				EvidenceProvenance.missingProvenance()
		);
		EvidenceTrustScore score = calculator.calculate(
				governanceIntegrationResult(policy),
				lineageIntegrationResult(
						EvidenceLineageStatus.INCOMPLETE,
						EvidenceLineageReason.MISSING_PROVENANCE,
						policy,
						OperationalUncertainty.HIGH
				)
		);

		assertThat(score.level()).isEqualTo(EvidenceTrustScoreLevel.LOW);
		assertThat(score.reason()).isEqualTo(EvidenceTrustScoreReason.MISSING_PROVENANCE);
	}

	@Test
	void shouldDowngradeForIncompleteLineage() {
		EvidenceGovernancePolicy policy = governancePolicy(
				EvidenceTrustLevel.TRUSTED,
				EvidenceIntegrityStatus.INTACT,
				EvidenceClassification.PUBLIC_SAFE,
				provenance(true, false, false)
		);
		EvidenceTrustScore score = calculator.calculate(
				governanceIntegrationResult(policy),
				lineageIntegrationResult(
						EvidenceLineageStatus.INCOMPLETE,
						EvidenceLineageReason.MISSING_COLLECTION_STAGE,
						policy,
						OperationalUncertainty.HIGH
				)
		);

		assertThat(score.level()).isEqualTo(EvidenceTrustScoreLevel.LOW);
		assertThat(score.reason()).isEqualTo(EvidenceTrustScoreReason.INCOMPLETE_LINEAGE);
	}

	@Test
	void shouldDowngradeForDegradedIntegrity() {
		EvidenceGovernancePolicy policy = governancePolicy(
				EvidenceTrustLevel.TRUSTED,
				EvidenceIntegrityStatus.DEGRADED,
				EvidenceClassification.PUBLIC_SAFE,
				provenance(true, false, false)
		);
		EvidenceTrustScore score = calculator.calculate(
				governanceIntegrationResult(policy),
				lineageIntegrationResult(
						EvidenceLineageStatus.COMPLETE,
						EvidenceLineageReason.UNKNOWN,
						policy,
						OperationalUncertainty.MODERATE
				)
		);

		assertThat(score.level()).isEqualTo(EvidenceTrustScoreLevel.MEDIUM);
		assertThat(score.reason()).isEqualTo(EvidenceTrustScoreReason.DEGRADED_INTEGRITY);
	}

	@Test
	void shouldReturnLowForContradictoryEvidence() {
		EvidenceGovernancePolicy policy = governancePolicy(
				EvidenceTrustLevel.TRUSTED,
				EvidenceIntegrityStatus.CONTRADICTORY,
				EvidenceClassification.PUBLIC_SAFE,
				provenance(true, false, false)
		);
		EvidenceTrustScore score = calculator.calculate(
				governanceIntegrationResult(policy),
				lineageIntegrationResult(
						EvidenceLineageStatus.PARTIAL,
						EvidenceLineageReason.CONTRADICTORY_EVIDENCE,
						policy,
						OperationalUncertainty.HIGH
				)
		);

		assertThat(score.level()).isEqualTo(EvidenceTrustScoreLevel.LOW);
		assertThat(score.reason()).isEqualTo(EvidenceTrustScoreReason.CONTRADICTORY_EVIDENCE);
	}

	@Test
	void shouldReturnUntrustedForBlockedEvidence() {
		EvidenceGovernancePolicy policy = governancePolicy(
				EvidenceTrustLevel.PARTIALLY_TRUSTED,
				EvidenceIntegrityStatus.DEGRADED,
				EvidenceClassification.BLOCKED,
				provenance(true, false, true)
		);
		EvidenceTrustScore score = calculator.calculate(
				governanceIntegrationResult(policy),
				lineageIntegrationResult(
						EvidenceLineageStatus.BLOCKED,
						EvidenceLineageReason.BLOCKED_EVIDENCE,
						policy,
						OperationalUncertainty.CRITICAL
				)
		);

		assertThat(score.level()).isEqualTo(EvidenceTrustScoreLevel.UNTRUSTED);
		assertThat(score.reason()).isEqualTo(EvidenceTrustScoreReason.BLOCKED_EVIDENCE);
	}

	@Test
	void shouldApplyRestrictedTrustHandlingForPaymentEvidence() {
		EvidenceGovernancePolicy policy = governancePolicy(
				EvidenceTrustLevel.TRUSTED,
				EvidenceIntegrityStatus.INTACT,
				EvidenceClassification.RESTRICTED,
				provenance(true, false, false)
		);
		EvidenceTrustScore score = calculator.calculate(
				governanceIntegrationResult(policy),
				lineageIntegrationResult(
						EvidenceLineageStatus.RESTRICTED,
						EvidenceLineageReason.PAYMENT_RESTRICTED_EVIDENCE,
						policy,
						OperationalUncertainty.HIGH
				)
		);

		assertThat(score.level()).isEqualTo(EvidenceTrustScoreLevel.MEDIUM);
		assertThat(score.reason()).isEqualTo(EvidenceTrustScoreReason.PAYMENT_RESTRICTED_EVIDENCE);
		assertThat(score.scope()).isEqualTo(EvidenceTrustScoreScope.PAYMENT_EVIDENCE);
	}

	@Test
	void shouldNotExposeRawPayloadVendorOrCredentials() {
		EvidenceTrustScore score = calculator.calculate(
				governanceIntegrationResult(
						governancePolicy(
								EvidenceTrustLevel.TRUSTED,
								EvidenceIntegrityStatus.INTACT,
								EvidenceClassification.PUBLIC_SAFE,
								provenance(true, false, false)
						)
				),
				lineageIntegrationResult(
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

		assertThat(score.exposesRawPayload()).isFalse();
		assertThat(score.exposesVendorDetail()).isFalse();
		assertThat(score.exposesCredentialConfiguration()).isFalse();
	}

	@Test
	void shouldRemainDeterministicAndNonNumeric() {
		assertThat(calculator.deterministicRuleBased()).isTrue();
		assertThat(calculator.numericScore()).isFalse();
		assertThat(calculator.weightedAlgorithm()).isFalse();
		assertThat(calculator.mlInference()).isFalse();
	}

	@Test
	void shouldRejectNullInputs() {
		EvidenceGovernanceIntegrationResult governance = governanceIntegrationResult(
				governancePolicy(
						EvidenceTrustLevel.TRUSTED,
						EvidenceIntegrityStatus.INTACT,
						EvidenceClassification.PUBLIC_SAFE,
						provenance(true, false, false)
				)
		);
		EvidenceLineageIntegrationResult lineage = lineageIntegrationResult(
				EvidenceLineageStatus.COMPLETE,
				EvidenceLineageReason.UNKNOWN,
				governance.governancePolicy(),
				OperationalUncertainty.LOW
		);

		assertThatThrownBy(() -> calculator.calculate(null, lineage))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("governanceIntegrationResult must not be null");
		assertThatThrownBy(() -> calculator.calculate(governance, null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("lineageIntegrationResult must not be null");
	}

	private EvidenceGovernanceIntegrationResult governanceIntegrationResult(
			EvidenceGovernancePolicy policy
	) {
		return new EvidenceGovernanceIntegrationResult(
				policy,
				new EvidenceRuntimeApiResponse(
						new EvidenceRuntimeSummaryView(
								EvidenceRuntimeSummaryStatus.HEALTHY,
								OperationalUncertainty.LOW,
								policy.classification() == EvidenceClassification.RESTRICTED
										? OperationalUncertainty.HIGH
										: OperationalUncertainty.LOW,
								false,
								EvidenceRuntimeSummaryReason.UNKNOWN,
								true,
								EvidenceCompleteness.COMPLETE
						),
						EvidenceRuntimeApiStatus.READABLE,
						EvidenceRuntimeApiRejectionReason.UNKNOWN
				),
				EvidenceGovernanceIntegrationStatus.INTEGRATED,
				EvidenceGovernanceIntegrationReason.UNKNOWN,
				EvidenceGovernanceIntegrationScope.API_EXPOSURE
		);
	}

	private EvidenceLineageIntegrationResult lineageIntegrationResult(
			EvidenceLineageStatus status,
			EvidenceLineageReason reason,
			EvidenceGovernancePolicy policy,
			OperationalUncertainty risk
	) {
		return new EvidenceLineageIntegrationResult(
				new EvidenceLineage(
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
				),
				new EvidenceRuntimeApiResponse(
						new EvidenceRuntimeSummaryView(
								status == EvidenceLineageStatus.INCOMPLETE
										? EvidenceRuntimeSummaryStatus.UNCERTAIN
										: EvidenceRuntimeSummaryStatus.HEALTHY,
								risk,
								policy.classification() == EvidenceClassification.RESTRICTED
										? OperationalUncertainty.HIGH
										: OperationalUncertainty.LOW,
								status != EvidenceLineageStatus.COMPLETE,
								reason == EvidenceLineageReason.CONTRADICTORY_EVIDENCE
										? EvidenceRuntimeSummaryReason.CONTRADICTORY_EVIDENCE
										: EvidenceRuntimeSummaryReason.UNKNOWN,
								status != EvidenceLineageStatus.INCOMPLETE,
								EvidenceCompleteness.COMPLETE
						),
						status == EvidenceLineageStatus.BLOCKED
								? EvidenceRuntimeApiStatus.REJECTED
								: EvidenceRuntimeApiStatus.READABLE,
						EvidenceRuntimeApiRejectionReason.UNKNOWN
				),
				status == EvidenceLineageStatus.BLOCKED
						? EvidenceLineageIntegrationStatus.BLOCKED
						: status == EvidenceLineageStatus.INCOMPLETE
								? EvidenceLineageIntegrationStatus.UNTRUSTED
								: status == EvidenceLineageStatus.RESTRICTED
										? EvidenceLineageIntegrationStatus.RESTRICTED
										: EvidenceLineageIntegrationStatus.INTEGRATED,
				reason == EvidenceLineageReason.MISSING_PROVENANCE
						? EvidenceLineageIntegrationReason.MISSING_PROVENANCE_LINEAGE
						: reason == EvidenceLineageReason.BLOCKED_EVIDENCE
								? EvidenceLineageIntegrationReason.BLOCKED_LINEAGE
								: reason == EvidenceLineageReason.PAYMENT_RESTRICTED_EVIDENCE
										? EvidenceLineageIntegrationReason.PAYMENT_LINEAGE_RESTRICTED
										: reason == EvidenceLineageReason.CONTRADICTORY_EVIDENCE
												? EvidenceLineageIntegrationReason.CONTRADICTORY_LINEAGE_RISK
												: reason == EvidenceLineageReason.GOVERNANCE_PROTECTED_EVIDENCE
														? EvidenceLineageIntegrationReason.RESTRICTED_LINEAGE
														: EvidenceLineageIntegrationReason.UNKNOWN,
				status == EvidenceLineageStatus.BLOCKED
						? EvidenceLineageIntegrationScope.API_BLOCKED
						: status == EvidenceLineageStatus.INCOMPLETE
								? EvidenceLineageIntegrationScope.TRUST_DOWNGRADED
								: status == EvidenceLineageStatus.RESTRICTED
										? EvidenceLineageIntegrationScope.OPERATOR_FACING_RESTRICTED
										: EvidenceLineageIntegrationScope.API_EXPOSURE
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
