package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class OperationalReliabilityLokiAdapterContractTest {

	@Test
	void shouldRemainLogsEvidenceSourceOnly() {
		LokiEvidenceQuery query = query(
				logsQuery(false),
				"{app=\"payments\"}",
				LokiLogSemanticType.ERROR_EVENT
		);

		assertThat(query.evidenceQuery().sourceType()).isEqualTo(EvidenceSourceType.LOGS);
		assertThat(query.logsEvidenceOnly()).isTrue();
	}

	@Test
	void shouldRejectNonLogsEvidenceSource() {
		assertThatThrownBy(() -> query(
				new EvidenceQuery(
						EvidenceSourceType.METRICS,
						"incident-1",
						Instant.parse("2026-05-29T00:00:00Z"),
						Instant.parse("2026-05-29T01:00:00Z"),
						false
				),
				"{app=\"payments\"}",
				LokiLogSemanticType.ERROR_EVENT
		))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Loki evidence query requires LOGS source");
	}

	@Test
	void shouldNotExposeRawLogPayload() {
		LokiEvidenceMapping mapping = mapping(
				"{app=\"payments\"}",
				LokiLogSemanticType.ERROR_EVENT,
				false,
				false,
				false,
				false,
				false,
				false
		);

		assertThat(mapping.exposesRawLogPayload()).isFalse();
		assertThat(query(logsQuery(false), "{app=\"payments\"}",
				LokiLogSemanticType.ERROR_EVENT).exposesRawLogPayload()).isFalse();
	}

	@Test
	void shouldConvertLogEventToNormalizedEvidenceSignalOnly() {
		LokiEvidenceMapping mapping = mapping(
				"{app=\"payments\"}",
				LokiLogSemanticType.ERROR_EVENT,
				false,
				false,
				false,
				false,
				false,
				false
		);

		assertThat(mapping.normalizedEvidenceOnly()).isTrue();
		assertThat(mapping.toEvidenceSignal().type()).isEqualTo(EvidenceSignalType.LOG);
	}

	@Test
	void shouldRejectSensitivePayloadExposure() {
		LokiEvidenceMapping mapping = mapping(
				"{app=\"payments\"}",
				LokiLogSemanticType.ERROR_EVENT,
				false,
				true,
				true,
				true,
				true,
				false
		);

		assertThat(mapping.suppressesSensitivePayload()).isFalse();
		assertThatThrownBy(mapping::toEvidenceSignal)
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("sensitive log payload must not be exposed as semantic evidence");
	}

	@Test
	void shouldRejectHighCardinalityLabelExposure() {
		LokiEvidenceMapping mapping = mapping(
				"{app=\"payments\"}",
				LokiLogSemanticType.WARN_EVENT,
				false,
				false,
				false,
				false,
				false,
				true
		);

		assertThat(mapping.suppressesHighCardinalityLabels()).isFalse();
		assertThatThrownBy(mapping::toEvidenceSignal)
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("high-cardinality labels must not be exposed as semantic evidence");
	}

	@Test
	void shouldKeepAdapterFailureAsUnknownOrFailedEvidenceInsteadOfSystemFailure() {
		LokiEvidenceAdapterContract adapter = new LokiEvidenceAdapterContract() {
			@Override
			public EvidenceQueryResult collect(LokiEvidenceQuery query) {
				return failed(query);
			}
		};

		EvidenceQueryResult failed = adapter.failed(
				query(logsQuery(false), "{app=\"payments\"}", LokiLogSemanticType.UNKNOWN)
		);
		EvidenceQueryResult unknown = adapter.unknown(
				query(logsQuery(false), "{app=\"payments\"}", LokiLogSemanticType.UNKNOWN)
		);

		assertThat(failed.status()).isEqualTo(EvidenceCollectionStatus.FAILED);
		assertThat(failed.systemFailure()).isFalse();
		assertThat(unknown.status()).isEqualTo(EvidenceCollectionStatus.UNKNOWN);
		assertThat(unknown.maintainsUncertainty()).isTrue();
	}

	@Test
	void shouldNotElevatePaymentRelatedLogWithoutSanitizedConsistencyMetadata() {
		LokiEvidenceMapping mapping = mapping(
				"{app=\"payments\"}",
				LokiLogSemanticType.PAYMENT_CONSISTENCY_EVENT,
				false,
				false,
				false,
				false,
				false,
				false
		);

		assertThat(mapping.paymentSafetyElevated()).isFalse();
		assertThat(mapping.toEvidenceSignal().type()).isEqualTo(EvidenceSignalType.LOG);
	}

	@Test
	void shouldElevatePaymentRelatedLogWhenSanitizedConsistencyMetadataExists() {
		LokiEvidenceMapping mapping = mapping(
				"{app=\"payments\"}",
				LokiLogSemanticType.PAYMENT_CONSISTENCY_EVENT,
				true,
				false,
				false,
				false,
				false,
				false
		);

		assertThat(mapping.paymentSafetyElevated()).isTrue();
		assertThat(mapping.toEvidenceSignal().type())
				.isEqualTo(EvidenceSignalType.PAYMENT_SAFETY);
	}

	@Test
	void shouldMapVerificationLogToVerificationSignal() {
		LokiEvidenceMapping mapping = mapping(
				"{app=\"payments\"}",
				LokiLogSemanticType.VERIFICATION_EVENT,
				false,
				false,
				false,
				false,
				false,
				false
		);

		assertThat(mapping.toEvidenceSignal().type())
				.isEqualTo(EvidenceSignalType.VERIFICATION);
	}

	@Test
	void shouldNotGrantExecutionOrRecommendationAuthority() {
		LokiEvidenceAdapterContract adapter = new LokiEvidenceAdapterContract() {
			@Override
			public EvidenceQueryResult collect(LokiEvidenceQuery query) {
				return unknown(query);
			}
		};

		assertThat(adapter.executionAuthority()).isFalse();
		assertThat(adapter.recommendationAuthority()).isFalse();
	}

	@Test
	void shouldNotMutatePortfolioKnowledgeSource() {
		LokiEvidenceAdapterContract adapter = new LokiEvidenceAdapterContract() {
			@Override
			public EvidenceQueryResult collect(LokiEvidenceQuery query) {
				return unknown(query);
			}
		};
		LokiEvidenceQuery query = query(
				logsQuery(false),
				"{app=\"payments\"}",
				LokiLogSemanticType.ERROR_EVENT
		);

		assertThat(query.mutatesPortfolioKnowledgeSource()).isFalse();
		assertThat(adapter.mutatesPortfolioKnowledgeSource()).isFalse();
	}

	@Test
	void shouldExposeSupportedLokiSemanticTypesAndRejectionReasons() {
		assertThat(LokiLogSemanticType.values()).containsExactly(
				LokiLogSemanticType.ERROR_EVENT,
				LokiLogSemanticType.WARN_EVENT,
				LokiLogSemanticType.TIMEOUT_EVENT,
				LokiLogSemanticType.RETRY_EVENT,
				LokiLogSemanticType.PAYMENT_CONSISTENCY_EVENT,
				LokiLogSemanticType.ROLLBACK_EVENT,
				LokiLogSemanticType.VERIFICATION_EVENT,
				LokiLogSemanticType.SECURITY_REDACTED_EVENT,
				LokiLogSemanticType.UNKNOWN
		);
		assertThat(LokiEvidenceRejectionReason.values()).containsExactly(
				LokiEvidenceRejectionReason.LOGS_SOURCE_REQUIRED,
				LokiEvidenceRejectionReason.SENSITIVE_PAYLOAD_FORBIDDEN,
				LokiEvidenceRejectionReason.HIGH_CARDINALITY_LABELS_FORBIDDEN,
				LokiEvidenceRejectionReason.PAYMENT_CONSISTENCY_METADATA_REQUIRED
		);
	}

	@Test
	void shouldReturnCollectedLogsAsNormalizedSemanticEvidence() {
		LokiEvidenceAdapterContract adapter = new LokiEvidenceAdapterContract() {
			@Override
			public EvidenceQueryResult collect(LokiEvidenceQuery query) {
				return collected(
						query,
						List.of(mapping(
								"{app=\"payments\"}",
								LokiLogSemanticType.ERROR_EVENT,
								false,
								false,
								false,
								false,
								false,
								false
						))
				);
			}
		};

		EvidenceQueryResult result = adapter.collect(query(
				logsQuery(false),
				"{app=\"payments\"}",
				LokiLogSemanticType.ERROR_EVENT
		));

		assertThat(result.sourceType()).isEqualTo(EvidenceSourceType.LOGS);
		assertThat(result.normalizedSemanticEvidenceOnly()).isTrue();
		assertThat(result.signals()).extracting(EvidenceSignal::type)
				.containsExactly(EvidenceSignalType.LOG);
	}

	private EvidenceQuery logsQuery(boolean paymentRelated) {
		return new EvidenceQuery(
				EvidenceSourceType.LOGS,
				"incident-1",
				Instant.parse("2026-05-29T00:00:00Z"),
				Instant.parse("2026-05-29T01:00:00Z"),
				paymentRelated
		);
	}

	private LokiEvidenceQuery query(
			EvidenceQuery evidenceQuery,
			String logSelector,
			LokiLogSemanticType semanticType
	) {
		return new LokiEvidenceQuery(
				evidenceQuery,
				logSelector,
				semanticType
		);
	}

	private LokiEvidenceMapping mapping(
			String logSelector,
			LokiLogSemanticType semanticType,
			boolean sanitizedConsistencyMetadataPresent,
			boolean customerPayloadExposed,
			boolean tokenExposed,
			boolean secretExposed,
			boolean internalIpExposed,
			boolean highCardinalityLabelsPresent
	) {
		return new LokiEvidenceMapping(
				logSelector,
				semanticType,
				"signal-" + semanticType.name().toLowerCase(),
				"summary-" + semanticType.name().toLowerCase(),
				sanitizedConsistencyMetadataPresent,
				customerPayloadExposed,
				tokenExposed,
				secretExposed,
				internalIpExposed,
				highCardinalityLabelsPresent,
				customerPayloadExposed
						|| tokenExposed
						|| secretExposed
						|| internalIpExposed
								? LokiEvidenceRejectionReason.SENSITIVE_PAYLOAD_FORBIDDEN
								: highCardinalityLabelsPresent
										? LokiEvidenceRejectionReason
												.HIGH_CARDINALITY_LABELS_FORBIDDEN
										: null
		);
	}
}
