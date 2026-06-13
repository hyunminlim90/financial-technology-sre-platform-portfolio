package com.fintech.sre.agent.runtime.reliability;

import java.util.List;
import java.util.Objects;

public interface TempoEvidenceAdapterContract {

	EvidenceQueryResult collect(TempoEvidenceQuery query);

	default EvidenceSourceType sourceType() {
		return EvidenceSourceType.TRACES;
	}

	default EvidenceQueryResult collected(
			TempoEvidenceQuery query,
			List<TempoEvidenceMapping> mappings
	) {
		Objects.requireNonNull(query, "query must not be null");
		Objects.requireNonNull(mappings, "mappings must not be null");

		return new EvidenceQueryResult(
				EvidenceSourceType.TRACES,
				EvidenceCollectionStatus.COLLECTED,
				mappings.stream()
						.filter(mapping -> mapping.rejectionReason() == null)
						.map(TempoEvidenceMapping::toEvidenceSignal)
						.toList(),
				hasPaymentConsistencyMetadata(mappings)
		);
	}

	default EvidenceQueryResult unknown(TempoEvidenceQuery query) {
		Objects.requireNonNull(query, "query must not be null");
		return new EvidenceQueryResult(
				EvidenceSourceType.TRACES,
				EvidenceCollectionStatus.UNKNOWN,
				List.of(),
				false
		);
	}

	default EvidenceQueryResult failed(TempoEvidenceQuery query) {
		Objects.requireNonNull(query, "query must not be null");
		return new EvidenceQueryResult(
				EvidenceSourceType.TRACES,
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
			List<TempoEvidenceMapping> mappings
	) {
		return mappings.stream().anyMatch(
				mapping -> mapping.semanticType()
						== TempoTraceSemanticType.PAYMENT_CONSISTENCY_TRACE
						&& mapping.sanitizedConsistencyMetadataPresent()
		);
	}
}
