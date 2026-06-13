package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class OperationalReliabilityEvidenceAdapterContractTest {

	@Test
	void shouldRemainEvidenceCollectorContractOnly() {
		assertThat(EvidenceAdapterPort.class.isInterface()).isTrue();
	}

	@Test
	void shouldRemainVendorNeutral() {
		EvidenceQuery query = query(EvidenceSourceType.METRICS, false);

		assertThat(query.vendorNeutral()).isTrue();
	}

	@Test
	void shouldReturnNormalizedSemanticEvidenceOnly() {
		EvidenceQueryResult result = new EvidenceQueryResult(
				EvidenceSourceType.LOGS,
				EvidenceCollectionStatus.COLLECTED,
				List.of(signal(EvidenceSignalType.LOG, "log-1")),
				false
		);

		assertThat(result.normalizedSemanticEvidenceOnly()).isTrue();
		assertThat(result.signals()).extracting(EvidenceSignal::type)
				.containsExactly(EvidenceSignalType.LOG);
	}

	@Test
	void shouldNotExposeRawObservabilityPayload() {
		EvidenceQueryResult result = new EvidenceQueryResult(
				EvidenceSourceType.TRACES,
				EvidenceCollectionStatus.PARTIAL,
				List.of(signal(EvidenceSignalType.TRACE, "trace-1")),
				false
		);

		assertThat(result.exposesRawObservabilityPayload()).isFalse();
	}

	@Test
	void shouldRequirePaymentConsistencyMetadataForPaymentEvidence() {
		assertThatThrownBy(() -> new EvidenceQueryResult(
				EvidenceSourceType.PAYMENT_CONSISTENCY,
				EvidenceCollectionStatus.COLLECTED,
				List.of(signal(EvidenceSignalType.PAYMENT_SAFETY, "payment-1")),
				false
		))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("payment consistency evidence requires consistency metadata");
	}

	@Test
	void shouldAllowPaymentEvidenceWhenConsistencyMetadataExists() {
		EvidenceQueryResult result = new EvidenceQueryResult(
				EvidenceSourceType.PAYMENT_CONSISTENCY,
				EvidenceCollectionStatus.COLLECTED,
				List.of(signal(EvidenceSignalType.PAYMENT_SAFETY, "payment-1")),
				true
		);

		assertThat(result.paymentConsistencyMetadataPresent()).isTrue();
	}

	@Test
	void shouldMaintainUncertaintyForUnknownObservabilityResult() {
		EvidenceQueryResult result = new EvidenceQueryResult(
				EvidenceSourceType.EVENTS,
				EvidenceCollectionStatus.UNKNOWN,
				List.of(),
				false
		);

		assertThat(result.maintainsUncertainty()).isTrue();
	}

	@Test
	void shouldNotTreatAdapterFailureAsSystemFailure() {
		EvidenceQueryResult result = new EvidenceQueryResult(
				EvidenceSourceType.DEPLOYMENT,
				EvidenceCollectionStatus.FAILED,
				List.of(),
				false
		);

		assertThat(result.systemFailure()).isFalse();
	}

	@Test
	void shouldNotGrantExecutionOrRecommendationAuthority() {
		EvidenceQueryResult result = new EvidenceQueryResult(
				EvidenceSourceType.VERIFICATION,
				EvidenceCollectionStatus.COLLECTED,
				List.of(signal(EvidenceSignalType.VERIFICATION, "verification-1")),
				false
		);

		assertThat(result.executionAuthority()).isFalse();
		assertThat(result.recommendationAuthority()).isFalse();
	}

	@Test
	void shouldNotMutatePortfolioKnowledgeSource() {
		EvidenceQuery query = query(EvidenceSourceType.ROLLBACK, false);
		EvidenceQueryResult result = new EvidenceQueryResult(
				EvidenceSourceType.ROLLBACK,
				EvidenceCollectionStatus.ABSENT,
				List.of(),
				false
		);

		assertThat(query.mutatesPortfolioKnowledgeSource()).isFalse();
		assertThat(result.mutatesPortfolioKnowledgeSource()).isFalse();
	}

	@Test
	void shouldExposeSupportedSourceTypesAndStatuses() {
		assertThat(EvidenceSourceType.values()).containsExactly(
				EvidenceSourceType.METRICS,
				EvidenceSourceType.LOGS,
				EvidenceSourceType.TRACES,
				EvidenceSourceType.EVENTS,
				EvidenceSourceType.DEPLOYMENT,
				EvidenceSourceType.ROLLBACK,
				EvidenceSourceType.VERIFICATION,
				EvidenceSourceType.PAYMENT_CONSISTENCY
		);
		assertThat(EvidenceCollectionStatus.values()).containsExactly(
				EvidenceCollectionStatus.COLLECTED,
				EvidenceCollectionStatus.PARTIAL,
				EvidenceCollectionStatus.ABSENT,
				EvidenceCollectionStatus.UNKNOWN,
				EvidenceCollectionStatus.FAILED
		);
	}

	@Test
	void shouldRejectInvalidQueryWindow() {
		assertThatThrownBy(() -> new EvidenceQuery(
				EvidenceSourceType.METRICS,
				"incident-1",
				Instant.parse("2026-05-29T01:00:00Z"),
				Instant.parse("2026-05-29T00:00:00Z"),
				false
		))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("from must not be after to");
	}

	private EvidenceQuery query(
			EvidenceSourceType sourceType,
			boolean paymentRelated
	) {
		return new EvidenceQuery(
				sourceType,
				"incident-1",
				Instant.parse("2026-05-29T00:00:00Z"),
				Instant.parse("2026-05-29T01:00:00Z"),
				paymentRelated
		);
	}

	private EvidenceSignal signal(
			EvidenceSignalType type,
			String signalId
	) {
		return new EvidenceSignal(type, signalId, "summary-" + signalId);
	}
}
