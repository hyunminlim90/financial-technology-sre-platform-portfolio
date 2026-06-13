package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class OperationalReliabilityEvidenceLineageTest {

	@Test
	void shouldRemainReadOnlyTraceabilityModel() {
		EvidenceLineage lineage = EvidenceLineage.trace(
				governancePolicy(
						EvidenceTrustLevel.TRUSTED,
						EvidenceIntegrityStatus.INTACT,
						EvidenceClassification.PUBLIC_SAFE,
						provenance(true, false, false)
				),
				true,
				true
		);

		assertThat(lineage.readOnlyTraceabilityModel()).isTrue();
		assertThat(lineage.mutatesEvidence()).isFalse();
		assertThat(lineage.recommendationAuthority()).isFalse();
		assertThat(lineage.executionAuthority()).isFalse();
		assertThat(lineage.mutatesPortfolioKnowledgeSource()).isFalse();
	}

	@Test
	void shouldTraceSourceToSummaryStages() {
		EvidenceLineage lineage = EvidenceLineage.trace(
				governancePolicy(
						EvidenceTrustLevel.TRUSTED,
						EvidenceIntegrityStatus.INTACT,
						EvidenceClassification.PUBLIC_SAFE,
						provenance(true, false, false)
				),
				true,
				true
		);

		assertThat(lineage.nodes()).containsExactly(
				EvidenceLineageNode.SOURCE,
				EvidenceLineageNode.ADAPTER,
				EvidenceLineageNode.ROUTING,
				EvidenceLineageNode.DISPATCH,
				EvidenceLineageNode.EXECUTION,
				EvidenceLineageNode.COLLECTION,
				EvidenceLineageNode.ASSESSMENT,
				EvidenceLineageNode.SUMMARY
		);
		assertThat(lineage.edges()).hasSize(7);
		assertThat(lineage.edges().get(0)).isEqualTo(
				new EvidenceLineageEdge(
						EvidenceLineageNode.SOURCE,
						EvidenceLineageNode.ADAPTER
				)
		);
	}

	@Test
	void shouldMarkMissingProvenanceAsIncomplete() {
		EvidenceLineage lineage = EvidenceLineage.trace(
				governancePolicy(
						EvidenceTrustLevel.UNTRUSTED,
						EvidenceIntegrityStatus.MISSING,
						EvidenceClassification.UNKNOWN,
						EvidenceProvenance.missingProvenance()
				),
				true,
				true
		);

		assertThat(lineage.status()).isEqualTo(EvidenceLineageStatus.INCOMPLETE);
		assertThat(lineage.reason()).isEqualTo(EvidenceLineageReason.MISSING_PROVENANCE);
	}

	@Test
	void shouldMarkBlockedEvidenceAsBlockedLineage() {
		EvidenceLineage lineage = EvidenceLineage.trace(
				governancePolicy(
						EvidenceTrustLevel.PARTIALLY_TRUSTED,
						EvidenceIntegrityStatus.DEGRADED,
						EvidenceClassification.BLOCKED,
						provenance(true, false, true)
				),
				true,
				true
		);

		assertThat(lineage.status()).isEqualTo(EvidenceLineageStatus.BLOCKED);
		assertThat(lineage.reason()).isEqualTo(EvidenceLineageReason.BLOCKED_EVIDENCE);
		assertThat(lineage.riskLevel()).isEqualTo(OperationalUncertainty.CRITICAL);
	}

	@Test
	void shouldMarkGovernanceProtectedEvidenceAsRestrictedLineage() {
		EvidenceLineage lineage = EvidenceLineage.trace(
				governancePolicy(
						EvidenceTrustLevel.TRUSTED,
						EvidenceIntegrityStatus.DEGRADED,
						EvidenceClassification.GOVERNANCE_PROTECTED,
						provenance(true, true, false)
				),
				true,
				true
		);

		assertThat(lineage.status()).isEqualTo(EvidenceLineageStatus.RESTRICTED);
		assertThat(lineage.reason())
				.isEqualTo(EvidenceLineageReason.GOVERNANCE_PROTECTED_EVIDENCE);
	}

	@Test
	void shouldPropagateContradictoryEvidenceAsLineageRisk() {
		EvidenceLineage lineage = EvidenceLineage.trace(
				governancePolicy(
						EvidenceTrustLevel.TRUSTED,
						EvidenceIntegrityStatus.CONTRADICTORY,
						EvidenceClassification.PUBLIC_SAFE,
						provenance(true, false, false)
				),
				true,
				true
		);

		assertThat(lineage.status()).isEqualTo(EvidenceLineageStatus.PARTIAL);
		assertThat(lineage.reason()).isEqualTo(EvidenceLineageReason.CONTRADICTORY_EVIDENCE);
		assertThat(lineage.riskLevel()).isEqualTo(OperationalUncertainty.HIGH);
	}

	@Test
	void shouldTreatPaymentEvidenceAsRestrictedLineageClassification() {
		EvidenceLineage lineage = EvidenceLineage.trace(
				governancePolicy(
						EvidenceTrustLevel.TRUSTED,
						EvidenceIntegrityStatus.INTACT,
						EvidenceClassification.RESTRICTED,
						provenance(true, false, false)
				),
				true,
				true
		);

		assertThat(lineage.status()).isEqualTo(EvidenceLineageStatus.RESTRICTED);
		assertThat(lineage.reason())
				.isEqualTo(EvidenceLineageReason.PAYMENT_RESTRICTED_EVIDENCE);
		assertThat(lineage.governancePolicy().classification())
				.isEqualTo(EvidenceClassification.RESTRICTED);
	}

	@Test
	void shouldMarkMissingCollectionOrAssessmentStagesAsIncomplete() {
		EvidenceLineage missingCollection = EvidenceLineage.trace(
				governancePolicy(
						EvidenceTrustLevel.TRUSTED,
						EvidenceIntegrityStatus.INTACT,
						EvidenceClassification.PUBLIC_SAFE,
						provenance(true, false, false)
				),
				false,
				true
		);
		EvidenceLineage missingAssessment = EvidenceLineage.trace(
				governancePolicy(
						EvidenceTrustLevel.TRUSTED,
						EvidenceIntegrityStatus.INTACT,
						EvidenceClassification.PUBLIC_SAFE,
						provenance(true, false, false)
				),
				true,
				false
		);

		assertThat(missingCollection.status()).isEqualTo(EvidenceLineageStatus.INCOMPLETE);
		assertThat(missingCollection.reason())
				.isEqualTo(EvidenceLineageReason.MISSING_COLLECTION_STAGE);
		assertThat(missingAssessment.status()).isEqualTo(EvidenceLineageStatus.INCOMPLETE);
		assertThat(missingAssessment.reason())
				.isEqualTo(EvidenceLineageReason.MISSING_ASSESSMENT_STAGE);
	}

	@Test
	void shouldRejectNullGovernancePolicy() {
		assertThatThrownBy(() -> EvidenceLineage.trace(null, true, true))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("governancePolicy must not be null");
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
