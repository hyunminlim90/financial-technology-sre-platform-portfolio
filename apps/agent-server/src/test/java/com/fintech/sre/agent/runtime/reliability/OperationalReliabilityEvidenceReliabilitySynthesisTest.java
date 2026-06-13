package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class OperationalReliabilityEvidenceReliabilitySynthesisTest {

	private final EvidenceReliabilitySynthesizer synthesizer =
			new EvidenceReliabilitySynthesizer();

	@Test
	void shouldRemainReadOnlyAndNonAuthoritative() {
		EvidenceReliability reliability = synthesizer.synthesize(confidence(
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
		));

		assertThat(reliability.readOnly()).isTrue();
		assertThat(reliability.mutatesEvidence()).isFalse();
		assertThat(reliability.recommendation()).isFalse();
		assertThat(reliability.executionPermission()).isFalse();
		assertThat(reliability.actionAdmissionResult()).isFalse();
		assertThat(reliability.mutatesPortfolioKnowledgeSource()).isFalse();
		assertThat(reliability.exposesRawPayload()).isFalse();
		assertThat(reliability.exposesVendorDetail()).isFalse();
		assertThat(reliability.exposesCredentialConfiguration()).isFalse();
	}

	@Test
	void shouldBlockReliabilityWhenGovernanceIsBlocked() {
		EvidenceReliability reliability = synthesizer.synthesize(confidence(
				EvidenceConfidenceLevel.HIGH,
				EvidenceConfidenceReason.CORROBORATING_EVIDENCE,
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
		));

		assertThat(reliability.level()).isEqualTo(EvidenceReliabilityLevel.BLOCKED);
		assertThat(reliability.reason()).isEqualTo(EvidenceReliabilityReason.GOVERNANCE_BLOCKED);
	}

	@Test
	void shouldDowngradeReliabilityForIncompleteLineage() {
		EvidenceReliability reliability = synthesizer.synthesize(confidence(
				EvidenceConfidenceLevel.MEDIUM,
				EvidenceConfidenceReason.PARTIAL_EVIDENCE,
				EvidenceConfidenceScope.ASSESSMENT,
				trustScore(
						EvidenceTrustScoreLevel.LOW,
						EvidenceTrustScoreReason.INCOMPLETE_LINEAGE,
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
								)
						)
				)
		));

		assertThat(reliability.level()).isEqualTo(EvidenceReliabilityLevel.LOW);
		assertThat(reliability.reason()).isEqualTo(EvidenceReliabilityReason.INCOMPLETE_LINEAGE);
	}

	@Test
	void shouldMarkUntrustedTrustAsUnreliable() {
		EvidenceReliability reliability = synthesizer.synthesize(confidence(
				EvidenceConfidenceLevel.MEDIUM,
				EvidenceConfidenceReason.PARTIAL_EVIDENCE,
				EvidenceConfidenceScope.EVIDENCE,
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
		));

		assertThat(reliability.level()).isEqualTo(EvidenceReliabilityLevel.UNRELIABLE);
		assertThat(reliability.reason()).isEqualTo(EvidenceReliabilityReason.UNTRUSTED_EVIDENCE);
	}

	@Test
	void shouldDisallowAssessmentCertaintyForInsufficientConfidence() {
		EvidenceReliability reliability = synthesizer.synthesize(confidence(
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
		));

		assertThat(reliability.level()).isEqualTo(EvidenceReliabilityLevel.LOW);
		assertThat(reliability.reason())
				.isEqualTo(EvidenceReliabilityReason.INSUFFICIENT_CONFIDENCE);
		assertThat(reliability.assessmentCertaintyAllowed()).isFalse();
	}

	@Test
	void shouldRequireAllEvidenceDimensionsForHighReliability() {
		EvidenceReliability reliability = synthesizer.synthesize(confidence(
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
		));

		assertThat(reliability.level()).isEqualTo(EvidenceReliabilityLevel.HIGH);
		assertThat(reliability.reason())
				.isEqualTo(EvidenceReliabilityReason.HIGH_RELIABILITY_EVIDENCE);
	}

	@Test
	void shouldPreservePaymentSafetyUncertainty() {
		EvidenceReliability reliability = synthesizer.synthesize(confidence(
				EvidenceConfidenceLevel.LOW,
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
		));

		assertThat(reliability.level()).isEqualTo(EvidenceReliabilityLevel.RESTRICTED);
		assertThat(reliability.reason())
				.isEqualTo(EvidenceReliabilityReason.PAYMENT_SAFETY_UNCERTAINTY);
		assertThat(reliability.paymentSafetyUncertainty()).isTrue();
	}

	@Test
	void shouldDowngradeContradictoryEvidence() {
		EvidenceReliability reliability = synthesizer.synthesize(confidence(
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
		));

		assertThat(reliability.level()).isEqualTo(EvidenceReliabilityLevel.LOW);
		assertThat(reliability.reason())
				.isEqualTo(EvidenceReliabilityReason.CONTRADICTORY_EVIDENCE);
	}

	@Test
	void shouldRejectNullConfidence() {
		assertThatThrownBy(() -> synthesizer.synthesize(null))
				.isInstanceOf(NullPointerException.class)
				.hasMessageContaining("confidence");
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
						status(governancePolicy),
						governanceReason(governancePolicy),
						EvidenceGovernanceIntegrationScope.API_EXPOSURE
				),
				new EvidenceLineageIntegrationResult(
						lineage,
						apiResponse,
						lineageStatus(lineage),
						lineageReason(lineage),
						EvidenceLineageIntegrationScope.API_EXPOSURE
				)
		);
	}

	private EvidenceGovernanceIntegrationStatus status(
			EvidenceGovernancePolicy governancePolicy
	) {
		return switch (governancePolicy.classification()) {
			case BLOCKED -> EvidenceGovernanceIntegrationStatus.BLOCKED;
			case GOVERNANCE_PROTECTED, RESTRICTED -> EvidenceGovernanceIntegrationStatus.RESTRICTED;
			case PUBLIC_SAFE, INTERNAL, UNKNOWN -> EvidenceGovernanceIntegrationStatus.INTEGRATED;
		};
	}

	private EvidenceGovernanceIntegrationReason governanceReason(
			EvidenceGovernancePolicy governancePolicy
	) {
		return switch (governancePolicy.classification()) {
			case BLOCKED -> EvidenceGovernanceIntegrationReason.BLOCKED_CLASSIFICATION;
			case GOVERNANCE_PROTECTED ->
					EvidenceGovernanceIntegrationReason.GOVERNANCE_PROTECTED_CLASSIFICATION;
			case RESTRICTED ->
					EvidenceGovernanceIntegrationReason.PAYMENT_RESTRICTED_CLASSIFICATION;
			case PUBLIC_SAFE, INTERNAL, UNKNOWN -> governancePolicy.provenance().provenanceMissing()
					? EvidenceGovernanceIntegrationReason.MISSING_PROVENANCE
					: EvidenceGovernanceIntegrationReason.UNKNOWN;
		};
	}

	private EvidenceLineageIntegrationStatus lineageStatus(
			EvidenceLineage lineage
	) {
		return switch (lineage.status()) {
			case BLOCKED -> EvidenceLineageIntegrationStatus.BLOCKED;
			case RESTRICTED -> EvidenceLineageIntegrationStatus.RESTRICTED;
			case INCOMPLETE -> EvidenceLineageIntegrationStatus.UNTRUSTED;
			case COMPLETE, PARTIAL -> EvidenceLineageIntegrationStatus.INTEGRATED;
			case UNKNOWN -> EvidenceLineageIntegrationStatus.UNKNOWN;
		};
	}

	private EvidenceLineageIntegrationReason lineageReason(
			EvidenceLineage lineage
	) {
		return switch (lineage.reason()) {
			case BLOCKED_EVIDENCE -> EvidenceLineageIntegrationReason.BLOCKED_LINEAGE;
			case GOVERNANCE_PROTECTED_EVIDENCE ->
					EvidenceLineageIntegrationReason.RESTRICTED_LINEAGE;
			case PAYMENT_RESTRICTED_EVIDENCE ->
					EvidenceLineageIntegrationReason.PAYMENT_LINEAGE_RESTRICTED;
			case CONTRADICTORY_EVIDENCE ->
					EvidenceLineageIntegrationReason.CONTRADICTORY_LINEAGE_RISK;
			case MISSING_PROVENANCE ->
					EvidenceLineageIntegrationReason.MISSING_PROVENANCE_LINEAGE;
			case MISSING_COLLECTION_STAGE, MISSING_ASSESSMENT_STAGE ->
					EvidenceLineageIntegrationReason.INCOMPLETE_LINEAGE;
			case UNKNOWN -> EvidenceLineageIntegrationReason.UNKNOWN;
		};
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
				status == EvidenceLineageStatus.BLOCKED
						? OperationalUncertainty.CRITICAL
						: status == EvidenceLineageStatus.INCOMPLETE
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
