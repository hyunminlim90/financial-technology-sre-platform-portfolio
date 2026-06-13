package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class OperationalReliabilityEvidenceAdapterSelectionTest {

	private final EvidenceAdapterSelector selector = new EvidenceAdapterSelector();

	@Test
	void shouldPreferAvailableAdapters() {
		EvidenceAdapterRegistry registry = EvidenceAdapterRegistry.of(List.of(
				registration("prometheus-available", EvidenceSourceType.METRICS,
						EvidenceAdapterAvailability.AVAILABLE, true),
				registration("prometheus-unavailable", EvidenceSourceType.METRICS,
						EvidenceAdapterAvailability.UNAVAILABLE, true)
		));

		EvidenceAdapterSelection selection = selector.select(
				registry,
				EvidenceSourceType.METRICS,
				EvidenceAdapterSelectionScope.STANDARD_EVIDENCE
		);

		assertThat(selection.selected()).isTrue();
		assertThat(selection.registrations())
				.extracting(registration -> registration.descriptor().adapterId())
				.containsExactly("prometheus-available");
	}

	@Test
	void shouldAllowMultipleAvailableAdaptersForSameSourceType() {
		EvidenceAdapterRegistry registry = EvidenceAdapterRegistry.of(List.of(
				registration("loki-a", EvidenceSourceType.LOGS,
						EvidenceAdapterAvailability.AVAILABLE, true),
				registration("loki-b", EvidenceSourceType.LOGS,
						EvidenceAdapterAvailability.AVAILABLE, false)
		));

		EvidenceAdapterSelection selection = selector.select(
				registry,
				EvidenceSourceType.LOGS,
				EvidenceAdapterSelectionScope.STANDARD_EVIDENCE
		);

		assertThat(selection.registrations()).hasSize(2);
	}

	@Test
	void shouldExcludeUnavailableAdaptersFromDefaultSelection() {
		EvidenceAdapterRegistry registry = EvidenceAdapterRegistry.of(List.of(
				registration("tempo-unavailable", EvidenceSourceType.TRACES,
						EvidenceAdapterAvailability.UNAVAILABLE, true)
		));

		EvidenceAdapterSelection selection = selector.select(
				registry,
				EvidenceSourceType.TRACES,
				EvidenceAdapterSelectionScope.STANDARD_EVIDENCE
		);

		assertThat(selection.selected()).isFalse();
		assertThat(selection.rejectionReason())
				.isEqualTo(EvidenceAdapterSelectionRejectionReason.NO_AVAILABLE_ADAPTER);
	}

	@Test
	void shouldAllowDeprecatedAdapterAsRestrictedFallback() {
		EvidenceAdapterRegistry registry = EvidenceAdapterRegistry.of(List.of(
				registration("prometheus-deprecated", EvidenceSourceType.METRICS,
						EvidenceAdapterAvailability.DEPRECATED, true)
		));

		EvidenceAdapterSelection selection = selector.select(
				registry,
				EvidenceSourceType.METRICS,
				EvidenceAdapterSelectionScope.RESTRICTED_FALLBACK
		);

		assertThat(selection.selected()).isTrue();
		assertThat(selection.restricted()).isTrue();
	}

	@Test
	void shouldAllowUnknownAdapterAsUncertainFallback() {
		EvidenceAdapterRegistry registry = EvidenceAdapterRegistry.of(List.of(
				registration("tempo-unknown", EvidenceSourceType.TRACES,
						EvidenceAdapterAvailability.UNKNOWN, true)
		));

		EvidenceAdapterSelection selection = selector.select(
				registry,
				EvidenceSourceType.TRACES,
				EvidenceAdapterSelectionScope.UNCERTAIN_FALLBACK
		);

		assertThat(selection.selected()).isTrue();
		assertThat(selection.uncertain()).isTrue();
	}

	@Test
	void shouldRequirePaymentEvidenceSupportForPaymentSelection() {
		EvidenceAdapterRegistry registry = EvidenceAdapterRegistry.of(List.of(
				registration("prometheus-no-payment", EvidenceSourceType.METRICS,
						EvidenceAdapterAvailability.AVAILABLE, false)
		));

		EvidenceAdapterSelection selection = selector.select(
				registry,
				EvidenceSourceType.METRICS,
				EvidenceAdapterSelectionScope.PAYMENT_CONSISTENCY_EVIDENCE
		);

		assertThat(selection.selected()).isFalse();
		assertThat(selection.rejectionReason()).isEqualTo(
				EvidenceAdapterSelectionRejectionReason.PAYMENT_EVIDENCE_NOT_SUPPORTED
		);
	}

	@Test
	void shouldSelectAvailablePaymentEvidenceAdapterWhenSupported() {
		EvidenceAdapterRegistry registry = EvidenceAdapterRegistry.of(List.of(
				registration("prometheus-payment", EvidenceSourceType.METRICS,
						EvidenceAdapterAvailability.AVAILABLE, true),
				registration("prometheus-generic", EvidenceSourceType.METRICS,
						EvidenceAdapterAvailability.AVAILABLE, false)
		));

		EvidenceAdapterSelection selection = selector.select(
				registry,
				EvidenceSourceType.METRICS,
				EvidenceAdapterSelectionScope.PAYMENT_CONSISTENCY_EVIDENCE
		);

		assertThat(selection.selected()).isTrue();
		assertThat(selection.registrations())
				.extracting(registration -> registration.descriptor().adapterId())
				.containsExactly("prometheus-payment");
	}

	@Test
	void shouldUseRegistryDiscoveryOnlyWithoutExecutingAdapter() {
		AtomicInteger invocations = new AtomicInteger();
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
						query -> {
							invocations.incrementAndGet();
							throw new AssertionError("adapter execution should not happen");
						}
				)
		));

		selector.select(
				registry,
				EvidenceSourceType.TRACES,
				EvidenceAdapterSelectionScope.STANDARD_EVIDENCE
		);

		assertThat(invocations.get()).isZero();
	}

	@Test
	void shouldNotGrantRecommendationOrExecutionAuthority() {
		EvidenceAdapterSelection selection = selector.select(
				EvidenceAdapterRegistry.of(List.of(
						registration("loki", EvidenceSourceType.LOGS,
								EvidenceAdapterAvailability.AVAILABLE, false)
				)),
				EvidenceSourceType.LOGS,
				EvidenceAdapterSelectionScope.STANDARD_EVIDENCE
		);

		assertThat(selection.recommendationAuthority()).isFalse();
		assertThat(selection.executionAuthority()).isFalse();
		assertThat(selector.recommendationAuthority()).isFalse();
		assertThat(selector.executionAuthority()).isFalse();
	}

	@Test
	void shouldTreatSelectionFailureAsEvidenceUncertaintyNotSystemFailure() {
		EvidenceAdapterSelection selection = selector.select(
				EvidenceAdapterRegistry.empty(),
				EvidenceSourceType.METRICS,
				EvidenceAdapterSelectionScope.STANDARD_EVIDENCE
		);

		assertThat(selection.selected()).isFalse();
		assertThat(selector.systemFailure()).isFalse();
		assertThat(selection.rejectionReason())
				.isEqualTo(EvidenceAdapterSelectionRejectionReason.NO_REGISTERED_ADAPTER);
	}

	@Test
	void shouldNotExposeRawCredentialOrConfiguration() {
		EvidenceAdapterSelection selection = selector.select(
				EvidenceAdapterRegistry.of(List.of(
						registration("prometheus", EvidenceSourceType.METRICS,
								EvidenceAdapterAvailability.AVAILABLE, true)
				)),
				EvidenceSourceType.METRICS,
				EvidenceAdapterSelectionScope.STANDARD_EVIDENCE
		);

		assertThat(selection.exposesRawCredentialOrConfiguration()).isFalse();
		assertThat(selection.registrations().get(0).descriptor()
				.exposesRawCredentialOrConfiguration()).isFalse();
	}

	@Test
	void shouldNotMutatePortfolioKnowledgeSource() {
		EvidenceAdapterSelection selection = selector.select(
				EvidenceAdapterRegistry.of(List.of(
						registration("tempo", EvidenceSourceType.TRACES,
								EvidenceAdapterAvailability.UNKNOWN, true)
				)),
				EvidenceSourceType.TRACES,
				EvidenceAdapterSelectionScope.UNCERTAIN_FALLBACK
		);

		assertThat(selector.mutatesPortfolioKnowledgeSource()).isFalse();
		assertThat(selection.mutatesPortfolioKnowledgeSource()).isFalse();
	}

	@Test
	void shouldExposeSupportedPoliciesScopesAndRejectionReasons() {
		assertThat(EvidenceAdapterSelectionPolicy.values()).containsExactly(
				EvidenceAdapterSelectionPolicy.PREFER_AVAILABLE,
				EvidenceAdapterSelectionPolicy.ALLOW_DEPRECATED_RESTRICTED,
				EvidenceAdapterSelectionPolicy.ALLOW_UNKNOWN_UNCERTAIN,
				EvidenceAdapterSelectionPolicy.REQUIRE_PAYMENT_EVIDENCE_SUPPORT
		);
		assertThat(EvidenceAdapterSelectionScope.values()).containsExactly(
				EvidenceAdapterSelectionScope.STANDARD_EVIDENCE,
				EvidenceAdapterSelectionScope.PAYMENT_CONSISTENCY_EVIDENCE,
				EvidenceAdapterSelectionScope.RESTRICTED_FALLBACK,
				EvidenceAdapterSelectionScope.UNCERTAIN_FALLBACK
		);
		assertThat(EvidenceAdapterSelectionRejectionReason.values()).containsExactly(
				EvidenceAdapterSelectionRejectionReason.NO_REGISTERED_ADAPTER,
				EvidenceAdapterSelectionRejectionReason.NO_AVAILABLE_ADAPTER,
				EvidenceAdapterSelectionRejectionReason.PAYMENT_EVIDENCE_NOT_SUPPORTED,
				EvidenceAdapterSelectionRejectionReason.INVALID_SELECTION_SCOPE,
				EvidenceAdapterSelectionRejectionReason.UNKNOWN
		);
	}

	private EvidenceAdapterRegistration registration(
			String adapterId,
			EvidenceSourceType sourceType,
			EvidenceAdapterAvailability availability,
			boolean supportsPaymentEvidence
	) {
		return new EvidenceAdapterRegistration(
				new EvidenceAdapterDescriptor(
						adapterId,
						adapterId,
						sourceType,
						availability,
						true,
						supportsPaymentEvidence
				),
				query -> new EvidenceQueryResult(
						query.sourceType(),
						EvidenceCollectionStatus.UNKNOWN,
						List.of(),
						false
				)
		);
	}
}
