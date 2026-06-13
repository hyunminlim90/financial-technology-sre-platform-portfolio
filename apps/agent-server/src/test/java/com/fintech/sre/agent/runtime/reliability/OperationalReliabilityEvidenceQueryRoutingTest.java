package com.fintech.sre.agent.runtime.reliability;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class OperationalReliabilityEvidenceQueryRoutingTest {

	private final EvidenceAdapterSelector selector = new EvidenceAdapterSelector();
	private final EvidenceQueryRouter router = new EvidenceQueryRouter();

	@Test
	void shouldConvertStandardSelectionToStandardRouteMetadata() {
		EvidenceQueryRoutingDecision decision = router.route(selection(
				EvidenceSourceType.METRICS,
				EvidenceAdapterAvailability.AVAILABLE,
				true,
				EvidenceAdapterSelectionScope.STANDARD_EVIDENCE
		));

		assertThat(decision.accepted()).isTrue();
		assertThat(decision.route().scope())
				.isEqualTo(EvidenceQueryRoutingScope.STANDARD_ROUTE);
	}

	@Test
	void shouldRequirePaymentSupportingRouteForPaymentConsistencyQuery() {
		EvidenceQueryRoutingDecision decision = router.route(selectionWithoutPaymentSupport(
				EvidenceSourceType.METRICS,
				EvidenceAdapterSelectionScope.PAYMENT_CONSISTENCY_EVIDENCE
		));

		assertThat(decision.accepted()).isFalse();
		assertThat(decision.route().rejectionReason())
				.isEqualTo(EvidenceQueryRoutingRejectionReason.PAYMENT_SUPPORT_REQUIRED);
	}

	@Test
	void shouldRejectUnavailableAdapterRoute() {
		EvidenceQueryRoutingDecision decision = router.route(selection(
				EvidenceSourceType.TRACES,
				EvidenceAdapterAvailability.UNAVAILABLE,
				true,
				EvidenceAdapterSelectionScope.STANDARD_EVIDENCE
		));

		assertThat(decision.accepted()).isFalse();
		assertThat(decision.route().scope())
				.isEqualTo(EvidenceQueryRoutingScope.REJECTED_ROUTE);
		assertThat(decision.route().rejectionReason())
				.isEqualTo(EvidenceQueryRoutingRejectionReason.UNAVAILABLE_ADAPTER);
	}

	@Test
	void shouldRouteDeprecatedAdapterAsRestrictedRoute() {
		EvidenceQueryRoutingDecision decision = router.route(selection(
				EvidenceSourceType.METRICS,
				EvidenceAdapterAvailability.DEPRECATED,
				true,
				EvidenceAdapterSelectionScope.RESTRICTED_FALLBACK
		));

		assertThat(decision.accepted()).isTrue();
		assertThat(decision.route().restricted()).isTrue();
		assertThat(decision.route().scope())
				.isEqualTo(EvidenceQueryRoutingScope.RESTRICTED_ROUTE);
	}

	@Test
	void shouldRouteUnknownAdapterAsUncertainRoute() {
		EvidenceQueryRoutingDecision decision = router.route(selection(
				EvidenceSourceType.LOGS,
				EvidenceAdapterAvailability.UNKNOWN,
				false,
				EvidenceAdapterSelectionScope.UNCERTAIN_FALLBACK
		));

		assertThat(decision.accepted()).isTrue();
		assertThat(decision.route().uncertain()).isTrue();
		assertThat(decision.route().scope())
				.isEqualTo(EvidenceQueryRoutingScope.UNCERTAIN_ROUTE);
	}

	@Test
	void shouldNotExecuteAdaptersWhileRouting() {
		AtomicInteger invocations = new AtomicInteger();
		EvidenceAdapterSelection selection = selector.select(
				EvidenceAdapterRegistry.of(List.of(
						new EvidenceAdapterRegistration(
								new EvidenceAdapterDescriptor(
										"prometheus",
										"prometheus",
										EvidenceSourceType.METRICS,
										EvidenceAdapterAvailability.AVAILABLE,
										true,
										true
								),
								query -> {
									invocations.incrementAndGet();
									throw new AssertionError("adapter execution should not happen");
								}
						)
				)),
				EvidenceSourceType.METRICS,
				EvidenceAdapterSelectionScope.STANDARD_EVIDENCE
		);

		router.route(selection);

		assertThat(invocations.get()).isZero();
	}

	@Test
	void shouldTreatRoutingFailureAsEvidenceUncertaintyNotSystemFailure() {
		EvidenceQueryRoutingDecision decision = router.route(
				selector.select(
						EvidenceAdapterRegistry.empty(),
						EvidenceSourceType.LOGS,
						EvidenceAdapterSelectionScope.STANDARD_EVIDENCE
				)
		);

		assertThat(decision.accepted()).isFalse();
		assertThat(router.systemFailure()).isFalse();
		assertThat(decision.route().rejectionReason())
				.isEqualTo(EvidenceQueryRoutingRejectionReason.NO_SELECTION_AVAILABLE);
	}

	@Test
	void shouldNotGrantRecommendationOrExecutionAuthority() {
		EvidenceQueryRoutingDecision decision = router.route(selection(
				EvidenceSourceType.TRACES,
				EvidenceAdapterAvailability.AVAILABLE,
				true,
				EvidenceAdapterSelectionScope.STANDARD_EVIDENCE
		));

		assertThat(decision.recommendationAuthority()).isFalse();
		assertThat(decision.executionPermission()).isFalse();
		assertThat(decision.route().recommendationAuthority()).isFalse();
		assertThat(decision.route().executionPermission()).isFalse();
		assertThat(router.recommendationAuthority()).isFalse();
		assertThat(router.executionAuthority()).isFalse();
	}

	@Test
	void shouldNotExposeRawCredentialOrConfiguration() {
		EvidenceQueryRoutingDecision decision = router.route(selection(
				EvidenceSourceType.METRICS,
				EvidenceAdapterAvailability.AVAILABLE,
				true,
				EvidenceAdapterSelectionScope.STANDARD_EVIDENCE
		));

		assertThat(decision.route().exposesRawCredentialOrConfiguration()).isFalse();
	}

	@Test
	void shouldNotMutatePortfolioKnowledgeSource() {
		EvidenceQueryRoutingDecision decision = router.route(selection(
				EvidenceSourceType.LOGS,
				EvidenceAdapterAvailability.AVAILABLE,
				false,
				EvidenceAdapterSelectionScope.STANDARD_EVIDENCE
		));

		assertThat(router.mutatesPortfolioKnowledgeSource()).isFalse();
		assertThat(decision.mutatesPortfolioKnowledgeSource()).isFalse();
		assertThat(decision.route().mutatesPortfolioKnowledgeSource()).isFalse();
	}

	@Test
	void shouldExposeSupportedRoutingScopesAndRejectionReasons() {
		assertThat(EvidenceQueryRoutingScope.values()).containsExactly(
				EvidenceQueryRoutingScope.STANDARD_ROUTE,
				EvidenceQueryRoutingScope.PAYMENT_CONSISTENCY_ROUTE,
				EvidenceQueryRoutingScope.RESTRICTED_ROUTE,
				EvidenceQueryRoutingScope.UNCERTAIN_ROUTE,
				EvidenceQueryRoutingScope.REJECTED_ROUTE
		);
		assertThat(EvidenceQueryRoutingRejectionReason.values()).containsExactly(
				EvidenceQueryRoutingRejectionReason.NO_SELECTION_AVAILABLE,
				EvidenceQueryRoutingRejectionReason.PAYMENT_SUPPORT_REQUIRED,
				EvidenceQueryRoutingRejectionReason.UNAVAILABLE_ADAPTER,
				EvidenceQueryRoutingRejectionReason.INVALID_ROUTE_SCOPE,
				EvidenceQueryRoutingRejectionReason.UNKNOWN
		);
	}

	private EvidenceAdapterSelection selection(
			EvidenceSourceType sourceType,
			EvidenceAdapterAvailability availability,
			boolean supportsPaymentEvidence,
			EvidenceAdapterSelectionScope scope
	) {
		return selector.select(
				EvidenceAdapterRegistry.of(List.of(
						registration(
								sourceType.name().toLowerCase() + "-" + availability.name().toLowerCase(),
								sourceType,
								availability,
								supportsPaymentEvidence
						)
				)),
				sourceType,
				scope
		);
	}

	private EvidenceAdapterSelection selectionWithoutPaymentSupport(
			EvidenceSourceType sourceType,
			EvidenceAdapterSelectionScope scope
	) {
		return selector.select(
				EvidenceAdapterRegistry.of(List.of(
						registration(
								sourceType.name().toLowerCase() + "-no-payment",
								sourceType,
								EvidenceAdapterAvailability.AVAILABLE,
								false
						)
				)),
				sourceType,
				scope
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
