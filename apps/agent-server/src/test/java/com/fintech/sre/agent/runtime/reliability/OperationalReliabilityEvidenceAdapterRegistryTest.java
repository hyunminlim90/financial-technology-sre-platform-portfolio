package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class OperationalReliabilityEvidenceAdapterRegistryTest {

	@Test
	void shouldAllowMultipleAdaptersForSameSourceType() {
		EvidenceAdapterRegistry registry = EvidenceAdapterRegistry.of(List.of(
				registration("prometheus-primary", "Prometheus Primary",
						EvidenceSourceType.METRICS,
						EvidenceAdapterAvailability.AVAILABLE),
				registration("prometheus-secondary", "Prometheus Secondary",
						EvidenceSourceType.METRICS,
						EvidenceAdapterAvailability.UNAVAILABLE)
		));

		assertThat(registry.findBySourceType(EvidenceSourceType.METRICS))
				.hasSize(2);
	}

	@Test
	void shouldAllowAvailableAndUnavailableRegistrations() {
		EvidenceAdapterRegistry registry = EvidenceAdapterRegistry.of(List.of(
				registration("loki-available", "Loki Available",
						EvidenceSourceType.LOGS,
						EvidenceAdapterAvailability.AVAILABLE),
				registration("loki-unavailable", "Loki Unavailable",
						EvidenceSourceType.LOGS,
						EvidenceAdapterAvailability.UNAVAILABLE)
		));

		assertThat(registry.descriptors()).extracting(EvidenceAdapterDescriptor::availability)
				.containsExactlyInAnyOrder(
						EvidenceAdapterAvailability.AVAILABLE,
						EvidenceAdapterAvailability.UNAVAILABLE
				);
	}

	@Test
	void shouldFindRegistrationsBySourceTypeOnly() {
		EvidenceAdapterRegistry registry = EvidenceAdapterRegistry.of(List.of(
				registration("prometheus", "Prometheus",
						EvidenceSourceType.METRICS,
						EvidenceAdapterAvailability.AVAILABLE),
				registration("loki", "Loki",
						EvidenceSourceType.LOGS,
						EvidenceAdapterAvailability.AVAILABLE),
				registration("tempo", "Tempo",
						EvidenceSourceType.TRACES,
						EvidenceAdapterAvailability.AVAILABLE)
		));

		assertThat(registry.findBySourceType(EvidenceSourceType.LOGS))
				.extracting(registration -> registration.descriptor().adapterId())
				.containsExactly("loki");
	}

	@Test
	void shouldNotExecuteAdaptersDuringRegistrationOrLookup() {
		AtomicInteger invocations = new AtomicInteger();
		EvidenceAdapterPort adapter = query -> {
			invocations.incrementAndGet();
			return new EvidenceQueryResult(
					query.sourceType(),
					EvidenceCollectionStatus.UNKNOWN,
					List.of(),
					false
			);
		};

		EvidenceAdapterRegistry registry = EvidenceAdapterRegistry.of(List.of(
				new EvidenceAdapterRegistration(
						new EvidenceAdapterDescriptor(
								"tempo",
								"Tempo",
								EvidenceSourceType.TRACES,
								EvidenceAdapterAvailability.AVAILABLE,
								true,
								true
						),
						adapter
				)
		));

		registry.findBySourceType(EvidenceSourceType.TRACES);
		registry.descriptors();

		assertThat(invocations.get()).isZero();
	}

	@Test
	void shouldKeepRegistrationAsDiscoveryMetadataOnly() {
		EvidenceAdapterRegistration registration = registration(
				"prometheus",
				"Prometheus",
				EvidenceSourceType.METRICS,
				EvidenceAdapterAvailability.AVAILABLE
		);

		assertThat(registration.runtimeDiscoveryMetadata()).isTrue();
		assertThat(registration.activation()).isFalse();
		assertThat(registration.executionPermission()).isFalse();
		assertThat(registration.healthCheckResult()).isFalse();
	}

	@Test
	void shouldNotGrantRecommendationOrExecutionAuthority() {
		EvidenceAdapterRegistry registry = EvidenceAdapterRegistry.of(List.of(
				registration("prometheus", "Prometheus",
						EvidenceSourceType.METRICS,
						EvidenceAdapterAvailability.AVAILABLE)
		));

		assertThat(registry.recommendationAuthority()).isFalse();
		assertThat(registry.executionAuthority()).isFalse();
	}

	@Test
	void shouldNotTreatRegistryFailureAsSystemFailure() {
		EvidenceAdapterRegistry registry = EvidenceAdapterRegistry.empty();

		assertThat(registry.systemFailure()).isFalse();
	}

	@Test
	void shouldNotMutatePortfolioKnowledgeSource() {
		EvidenceAdapterRegistry registry = EvidenceAdapterRegistry.of(List.of(
				registration("tempo", "Tempo",
						EvidenceSourceType.TRACES,
						EvidenceAdapterAvailability.UNKNOWN)
		));

		assertThat(registry.mutatesPortfolioKnowledgeSource()).isFalse();
	}

	@Test
	void shouldExposeVendorNeutralDescriptorOnly() {
		EvidenceAdapterDescriptor descriptor = new EvidenceAdapterDescriptor(
				"datadog-metrics",
				"Datadog Metrics",
				EvidenceSourceType.METRICS,
				EvidenceAdapterAvailability.UNKNOWN,
				true,
				true
		);

		assertThat(descriptor.exposesRawCredentialOrConfiguration()).isFalse();
	}

	@Test
	void shouldRejectDuplicateAdapterId() {
		assertThatThrownBy(() -> EvidenceAdapterRegistry.of(List.of(
				registration("prometheus", "Prometheus A",
						EvidenceSourceType.METRICS,
						EvidenceAdapterAvailability.AVAILABLE),
				registration("prometheus", "Prometheus B",
						EvidenceSourceType.METRICS,
						EvidenceAdapterAvailability.UNAVAILABLE)
		)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage(EvidenceAdapterRejectionReason.DUPLICATE_ADAPTER_ID.name());
	}

	@Test
	void shouldRejectInvalidDescriptor() {
		assertThatThrownBy(() -> EvidenceAdapterRegistry.of(List.of(
				new EvidenceAdapterRegistration(
						new EvidenceAdapterDescriptor(
								"cloudwatch",
								"CloudWatch",
								EvidenceSourceType.EVENTS,
								EvidenceAdapterAvailability.UNKNOWN,
								false,
								false
						),
						null
				)
		)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage(EvidenceAdapterRejectionReason.INVALID_DESCRIPTOR.name());
	}

	@Test
	void shouldExposeSupportedAvailabilitiesAndRejectionReasons() {
		assertThat(EvidenceAdapterAvailability.values()).containsExactly(
				EvidenceAdapterAvailability.AVAILABLE,
				EvidenceAdapterAvailability.UNAVAILABLE,
				EvidenceAdapterAvailability.UNKNOWN,
				EvidenceAdapterAvailability.DEPRECATED
		);
		assertThat(EvidenceAdapterRejectionReason.values()).containsExactly(
				EvidenceAdapterRejectionReason.DUPLICATE_ADAPTER_ID,
				EvidenceAdapterRejectionReason.UNSUPPORTED_SOURCE_TYPE,
				EvidenceAdapterRejectionReason.INVALID_DESCRIPTOR,
				EvidenceAdapterRejectionReason.DEPRECATED_ADAPTER,
				EvidenceAdapterRejectionReason.UNKNOWN
		);
	}

	private EvidenceAdapterRegistration registration(
			String adapterId,
			String adapterName,
			EvidenceSourceType sourceType,
			EvidenceAdapterAvailability availability
	) {
		return new EvidenceAdapterRegistration(
				new EvidenceAdapterDescriptor(
						adapterId,
						adapterName,
						sourceType,
						availability,
						true,
						sourceType == EvidenceSourceType.METRICS
								|| sourceType == EvidenceSourceType.LOGS
								|| sourceType == EvidenceSourceType.TRACES
				),
				query -> new EvidenceQueryResult(
						query.sourceType(),
						EvidenceCollectionStatus.UNKNOWN,
						List.of(new EvidenceSignal(
								signalTypeFor(sourceType),
								adapterId + "-signal",
								"summary-" + adapterId
						)),
						false
				)
		);
	}

	private EvidenceSignalType signalTypeFor(EvidenceSourceType sourceType) {
		return switch (sourceType) {
			case METRICS -> EvidenceSignalType.METRIC;
			case LOGS -> EvidenceSignalType.LOG;
			case TRACES -> EvidenceSignalType.TRACE;
			case VERIFICATION -> EvidenceSignalType.VERIFICATION;
			case EVENTS, DEPLOYMENT, ROLLBACK, PAYMENT_CONSISTENCY ->
					EvidenceSignalType.TIMELINE;
		};
	}
}
