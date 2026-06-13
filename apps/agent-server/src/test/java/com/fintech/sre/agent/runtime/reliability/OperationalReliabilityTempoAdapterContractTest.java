package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class OperationalReliabilityTempoAdapterContractTest {

	@Test
	void shouldRemainTracesEvidenceSourceOnly() {
		TempoEvidenceQuery query = query(
				tracesQuery(false),
				"{ resource.service.name = \"payments\" }",
				TempoTraceSemanticType.ERROR_TRACE
		);

		assertThat(query.evidenceQuery().sourceType()).isEqualTo(EvidenceSourceType.TRACES);
		assertThat(query.tracesEvidenceOnly()).isTrue();
	}

	@Test
	void shouldRejectNonTracesEvidenceSource() {
		assertThatThrownBy(() -> query(
				new EvidenceQuery(
						EvidenceSourceType.LOGS,
						"incident-1",
						Instant.parse("2026-05-29T00:00:00Z"),
						Instant.parse("2026-05-29T01:00:00Z"),
						false
				),
				"{ resource.service.name = \"payments\" }",
				TempoTraceSemanticType.ERROR_TRACE
		))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Tempo evidence query requires TRACES source");
	}

	@Test
	void shouldNotExposeRawTracePayload() {
		TempoEvidenceMapping mapping = mapping(
				"{ resource.service.name = \"payments\" }",
				TempoTraceSemanticType.ERROR_TRACE,
				false,
				false,
				false,
				false,
				false,
				false
		);

		assertThat(mapping.exposesRawTracePayload()).isFalse();
		assertThat(query(
				tracesQuery(false),
				"{ resource.service.name = \"payments\" }",
				TempoTraceSemanticType.ERROR_TRACE
		).exposesRawTracePayload()).isFalse();
	}

	@Test
	void shouldConvertTraceEventToNormalizedEvidenceSignalOnly() {
		TempoEvidenceMapping mapping = mapping(
				"{ resource.service.name = \"payments\" }",
				TempoTraceSemanticType.LATENCY_TRACE,
				false,
				false,
				false,
				false,
				false,
				false
		);

		assertThat(mapping.normalizedEvidenceOnly()).isTrue();
		assertThat(mapping.toEvidenceSignal().type()).isEqualTo(EvidenceSignalType.TRACE);
	}

	@Test
	void shouldRejectSensitivePayloadExposure() {
		TempoEvidenceMapping mapping = mapping(
				"{ resource.service.name = \"payments\" }",
				TempoTraceSemanticType.ERROR_TRACE,
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
				.hasMessage("sensitive trace payload must not be exposed as semantic evidence");
	}

	@Test
	void shouldRejectHighCardinalityIdentifierExposure() {
		TempoEvidenceMapping mapping = mapping(
				"{ resource.service.name = \"payments\" }",
				TempoTraceSemanticType.DEPENDENCY_TRACE,
				false,
				false,
				false,
				false,
				false,
				true
		);

		assertThat(mapping.suppressesHighCardinalityIdentifiers()).isFalse();
		assertThatThrownBy(mapping::toEvidenceSignal)
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("high-cardinality trace identifiers must not be exposed as semantic evidence");
	}

	@Test
	void shouldKeepAdapterFailureAsUnknownOrFailedEvidenceInsteadOfSystemFailure() {
		TempoEvidenceAdapterContract adapter = new TempoEvidenceAdapterContract() {
			@Override
			public EvidenceQueryResult collect(TempoEvidenceQuery query) {
				return failed(query);
			}
		};

		EvidenceQueryResult failed = adapter.failed(
				query(
						tracesQuery(false),
						"{ resource.service.name = \"payments\" }",
						TempoTraceSemanticType.UNKNOWN
				)
		);
		EvidenceQueryResult unknown = adapter.unknown(
				query(
						tracesQuery(false),
						"{ resource.service.name = \"payments\" }",
						TempoTraceSemanticType.UNKNOWN
				)
		);

		assertThat(failed.status()).isEqualTo(EvidenceCollectionStatus.FAILED);
		assertThat(failed.systemFailure()).isFalse();
		assertThat(unknown.status()).isEqualTo(EvidenceCollectionStatus.UNKNOWN);
		assertThat(unknown.maintainsUncertainty()).isTrue();
	}

	@Test
	void shouldNotElevatePaymentRelatedTraceWithoutSanitizedConsistencyMetadata() {
		TempoEvidenceMapping mapping = mapping(
				"{ resource.service.name = \"payments\" }",
				TempoTraceSemanticType.PAYMENT_CONSISTENCY_TRACE,
				false,
				false,
				false,
				false,
				false,
				false
		);

		assertThat(mapping.paymentSafetyElevated()).isFalse();
		assertThat(mapping.toEvidenceSignal().type()).isEqualTo(EvidenceSignalType.TRACE);
	}

	@Test
	void shouldElevatePaymentRelatedTraceWhenSanitizedConsistencyMetadataExists() {
		TempoEvidenceMapping mapping = mapping(
				"{ resource.service.name = \"payments\" }",
				TempoTraceSemanticType.PAYMENT_CONSISTENCY_TRACE,
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
	void shouldMapVerificationTraceToVerificationSignal() {
		TempoEvidenceMapping mapping = mapping(
				"{ resource.service.name = \"payments\" }",
				TempoTraceSemanticType.VERIFICATION_TRACE,
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
		TempoEvidenceAdapterContract adapter = new TempoEvidenceAdapterContract() {
			@Override
			public EvidenceQueryResult collect(TempoEvidenceQuery query) {
				return unknown(query);
			}
		};

		assertThat(adapter.executionAuthority()).isFalse();
		assertThat(adapter.recommendationAuthority()).isFalse();
	}

	@Test
	void shouldNotMutatePortfolioKnowledgeSource() {
		TempoEvidenceAdapterContract adapter = new TempoEvidenceAdapterContract() {
			@Override
			public EvidenceQueryResult collect(TempoEvidenceQuery query) {
				return unknown(query);
			}
		};
		TempoEvidenceQuery query = query(
				tracesQuery(false),
				"{ resource.service.name = \"payments\" }",
				TempoTraceSemanticType.ERROR_TRACE
		);

		assertThat(query.mutatesPortfolioKnowledgeSource()).isFalse();
		assertThat(adapter.mutatesPortfolioKnowledgeSource()).isFalse();
	}

	@Test
	void shouldExposeSupportedTempoSemanticTypesAndRejectionReasons() {
		assertThat(TempoTraceSemanticType.values()).containsExactly(
				TempoTraceSemanticType.LATENCY_TRACE,
				TempoTraceSemanticType.ERROR_TRACE,
				TempoTraceSemanticType.TIMEOUT_TRACE,
				TempoTraceSemanticType.RETRY_TRACE,
				TempoTraceSemanticType.DEPENDENCY_TRACE,
				TempoTraceSemanticType.PAYMENT_CONSISTENCY_TRACE,
				TempoTraceSemanticType.ROLLBACK_TRACE,
				TempoTraceSemanticType.VERIFICATION_TRACE,
				TempoTraceSemanticType.UNKNOWN
		);
		assertThat(TempoEvidenceRejectionReason.values()).containsExactly(
				TempoEvidenceRejectionReason.TRACES_SOURCE_REQUIRED,
				TempoEvidenceRejectionReason.SENSITIVE_PAYLOAD_FORBIDDEN,
				TempoEvidenceRejectionReason.HIGH_CARDINALITY_IDENTIFIER_FORBIDDEN,
				TempoEvidenceRejectionReason.PAYMENT_CONSISTENCY_METADATA_REQUIRED
		);
	}

	@Test
	void shouldReturnCollectedTracesAsNormalizedSemanticEvidence() {
		TempoEvidenceAdapterContract adapter = new TempoEvidenceAdapterContract() {
			@Override
			public EvidenceQueryResult collect(TempoEvidenceQuery query) {
				return collected(
						query,
						List.of(mapping(
								"{ resource.service.name = \"payments\" }",
								TempoTraceSemanticType.ERROR_TRACE,
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
				tracesQuery(false),
				"{ resource.service.name = \"payments\" }",
				TempoTraceSemanticType.ERROR_TRACE
		));

		assertThat(result.sourceType()).isEqualTo(EvidenceSourceType.TRACES);
		assertThat(result.normalizedSemanticEvidenceOnly()).isTrue();
		assertThat(result.signals()).extracting(EvidenceSignal::type)
				.containsExactly(EvidenceSignalType.TRACE);
	}

	private EvidenceQuery tracesQuery(boolean paymentRelated) {
		return new EvidenceQuery(
				EvidenceSourceType.TRACES,
				"incident-1",
				Instant.parse("2026-05-29T00:00:00Z"),
				Instant.parse("2026-05-29T01:00:00Z"),
				paymentRelated
		);
	}

	private TempoEvidenceQuery query(
			EvidenceQuery evidenceQuery,
			String traceSelector,
			TempoTraceSemanticType semanticType
	) {
		return new TempoEvidenceQuery(
				evidenceQuery,
				traceSelector,
				semanticType
		);
	}

	private TempoEvidenceMapping mapping(
			String traceSelector,
			TempoTraceSemanticType semanticType,
			boolean sanitizedConsistencyMetadataPresent,
			boolean customerPayloadExposed,
			boolean tokenExposed,
			boolean secretExposed,
			boolean internalIpExposed,
			boolean highCardinalityIdentifierPresent
	) {
		return new TempoEvidenceMapping(
				traceSelector,
				semanticType,
				"signal-" + semanticType.name().toLowerCase(),
				"summary-" + semanticType.name().toLowerCase(),
				sanitizedConsistencyMetadataPresent,
				customerPayloadExposed,
				tokenExposed,
				secretExposed,
				internalIpExposed,
				highCardinalityIdentifierPresent,
				customerPayloadExposed
						|| tokenExposed
						|| secretExposed
						|| internalIpExposed
								? TempoEvidenceRejectionReason.SENSITIVE_PAYLOAD_FORBIDDEN
								: highCardinalityIdentifierPresent
										? TempoEvidenceRejectionReason
												.HIGH_CARDINALITY_IDENTIFIER_FORBIDDEN
										: null
		);
	}
}
