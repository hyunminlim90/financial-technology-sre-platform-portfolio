package com.fintech.sre.agent.runtime.reliability;

import java.util.List;
import java.util.Objects;

public interface PrometheusEvidenceAdapterContract {

	EvidenceQueryResult collect(PrometheusEvidenceQuery query);

	default EvidenceSourceType sourceType() {
		return EvidenceSourceType.METRICS;
	}

	default EvidenceQueryResult collected(
			PrometheusEvidenceQuery query,
			List<PrometheusEvidenceMapping> mappings
	) {
		Objects.requireNonNull(query, "query must not be null");
		Objects.requireNonNull(mappings, "mappings must not be null");

		return new EvidenceQueryResult(
				EvidenceSourceType.METRICS,
				EvidenceCollectionStatus.COLLECTED,
				mappings.stream()
						.filter(mapping -> mapping.rejectionReason() == null)
						.map(PrometheusEvidenceMapping::toEvidenceSignal)
						.toList(),
				hasPaymentConsistencyMetadata(mappings)
		);
	}

	default EvidenceQueryResult unknown(PrometheusEvidenceQuery query) {
		Objects.requireNonNull(query, "query must not be null");
		return new EvidenceQueryResult(
				EvidenceSourceType.METRICS,
				EvidenceCollectionStatus.UNKNOWN,
				List.of(),
				false
		);
	}

	default EvidenceQueryResult failed(PrometheusEvidenceQuery query) {
		Objects.requireNonNull(query, "query must not be null");
		return new EvidenceQueryResult(
				EvidenceSourceType.METRICS,
				EvidenceCollectionStatus.FAILED,
				List.of(),
				false
		);
	}

	default boolean executionAuthority() {
		return false;
	}

	default boolean recommendationAuthority() {
		return false;
	}

	default boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}

	private boolean hasPaymentConsistencyMetadata(
			List<PrometheusEvidenceMapping> mappings
	) {
		return mappings.stream().anyMatch(
				mapping -> mapping.semanticType()
						== PrometheusMetricSemanticType.PAYMENT_CONSISTENCY
						&& mapping.paymentConsistencyMetadataPresent()
		);
	}
}
