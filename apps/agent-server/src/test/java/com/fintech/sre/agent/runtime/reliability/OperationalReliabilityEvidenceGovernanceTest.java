package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class OperationalReliabilityEvidenceGovernanceTest {

	@Test
	void shouldRemainPolicyOnlyAndNonAuthoritative() {
		EvidenceGovernancePolicy policy = EvidenceGovernancePolicy.govern(
				evidence(EvidenceSourceType.METRICS, false),
				provenance(EvidenceSourceType.METRICS, true, false, false),
				false
		);

		assertThat(policy.policyOnly()).isTrue();
		assertThat(policy.recommendationAuthority()).isFalse();
		assertThat(policy.executionAuthority()).isFalse();
		assertThat(policy.mutatesEvidence()).isFalse();
		assertThat(policy.mutatesPortfolioKnowledgeSource()).isFalse();
	}

	@Test
	void shouldDowngradeTrustForUnknownProvenance() {
		EvidenceGovernancePolicy policy = EvidenceGovernancePolicy.govern(
				evidence(EvidenceSourceType.LOGS, false),
				new EvidenceProvenance(
						EvidenceSourceType.LOGS,
						"",
						Instant.parse("2026-05-31T00:00:00Z"),
						true,
						false,
						false
				),
				false
		);

		assertThat(policy.trustLevel()).isEqualTo(EvidenceTrustLevel.UNKNOWN);
	}

	@Test
	void shouldMarkMissingProvenanceAsUntrusted() {
		EvidenceGovernancePolicy policy = EvidenceGovernancePolicy.govern(
				evidence(EvidenceSourceType.TRACES, false),
				EvidenceProvenance.missingProvenance(),
				false
		);

		assertThat(policy.trustLevel()).isEqualTo(EvidenceTrustLevel.UNTRUSTED);
		assertThat(policy.integrityStatus()).isEqualTo(EvidenceIntegrityStatus.MISSING);
	}

	@Test
	void shouldTreatContradictoryEvidenceAsDegradedIntegrity() {
		EvidenceGovernancePolicy policy = EvidenceGovernancePolicy.govern(
				evidence(EvidenceSourceType.EVENTS, false),
				provenance(EvidenceSourceType.EVENTS, true, false, false),
				true
		);

		assertThat(policy.integrityStatus()).isEqualTo(EvidenceIntegrityStatus.CONTRADICTORY);
	}

	@Test
	void shouldClassifyPaymentEvidenceAsRestricted() {
		EvidenceGovernancePolicy policy = EvidenceGovernancePolicy.govern(
				evidence(EvidenceSourceType.PAYMENT_CONSISTENCY, true),
				provenance(EvidenceSourceType.PAYMENT_CONSISTENCY, true, false, false),
				false
		);

		assertThat(policy.classification()).isEqualTo(EvidenceClassification.RESTRICTED);
	}

	@Test
	void shouldClassifyRawPayloadEvidenceAsGovernanceProtected() {
		EvidenceGovernancePolicy policy = EvidenceGovernancePolicy.govern(
				evidence(EvidenceSourceType.METRICS, false),
				provenance(EvidenceSourceType.METRICS, true, true, false),
				false
		);

		assertThat(policy.classification())
				.isEqualTo(EvidenceClassification.GOVERNANCE_PROTECTED);
		assertThat(policy.operatorFacingAllowed()).isFalse();
	}

	@Test
	void shouldBlockSensitiveEvidence() {
		EvidenceGovernancePolicy policy = EvidenceGovernancePolicy.govern(
				evidence(EvidenceSourceType.LOGS, false),
				provenance(EvidenceSourceType.LOGS, true, false, true),
				false
		);

		assertThat(policy.classification()).isEqualTo(EvidenceClassification.BLOCKED);
		assertThat(policy.operatorFacingAllowed()).isFalse();
	}

	@Test
	void shouldAllowOnlySanitizedEvidenceForOperatorFacingExposure() {
		EvidenceGovernancePolicy unsanitizedPolicy = EvidenceGovernancePolicy.govern(
				evidence(EvidenceSourceType.VERIFICATION, false),
				provenance(EvidenceSourceType.VERIFICATION, false, false, false),
				false
		);
		EvidenceGovernancePolicy sanitizedPolicy = EvidenceGovernancePolicy.govern(
				evidence(EvidenceSourceType.VERIFICATION, false),
				provenance(EvidenceSourceType.VERIFICATION, true, false, false),
				false
		);

		assertThat(unsanitizedPolicy.operatorFacingAllowed()).isFalse();
		assertThat(sanitizedPolicy.operatorFacingAllowed()).isTrue();
	}

	@Test
	void shouldRejectNullEvidenceOrProvenance() {
		assertThatThrownBy(() -> EvidenceGovernancePolicy.govern(
				null,
				provenance(EvidenceSourceType.METRICS, true, false, false),
				false
		))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("evidence must not be null");
		assertThatThrownBy(() -> EvidenceGovernancePolicy.govern(
				evidence(EvidenceSourceType.METRICS, false),
				null,
				false
		))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("provenance must not be null");
	}

	private EvidenceQueryResult evidence(
			EvidenceSourceType sourceType,
			boolean paymentConsistencyMetadataPresent
	) {
		return new EvidenceQueryResult(
				sourceType,
				EvidenceCollectionStatus.COLLECTED,
				List.of(new EvidenceSignal(EvidenceSignalType.METRIC, "signal-1", "summary-1")),
				paymentConsistencyMetadataPresent
		);
	}

	private EvidenceProvenance provenance(
			EvidenceSourceType sourceType,
			boolean sanitized,
			boolean rawPayloadPresent,
			boolean sensitiveDataPresent
	) {
		return new EvidenceProvenance(
				sourceType,
				"adapter-1",
				Instant.parse("2026-05-31T00:00:00Z"),
				sanitized,
				rawPayloadPresent,
				sensitiveDataPresent
		);
	}
}
