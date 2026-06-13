package com.fintech.sre.agent.runtime.reliability;

import java.util.List;
import java.util.Objects;

public interface LokiEvidenceAdapterContract {

	EvidenceQueryResult collect(LokiEvidenceQuery query);

	default EvidenceSourceType sourceType() {
		return EvidenceSourceType.LOGS;
	}

	default EvidenceQueryResult collected(
			LokiEvidenceQuery query,
			List<LokiEvidenceMapping> mappings
	) {
		Objects.requireNonNull(query, "query must not be null");
		Objects.requireNonNull(mappings, "mappings must not be null");

		return new EvidenceQueryResult(
				EvidenceSourceType.LOGS,
				EvidenceCollectionStatus.COLLECTED,
				mappings.stream()
						.filter(mapping -> mapping.rejectionReason() == null)
						.map(LokiEvidenceMapping::toEvidenceSignal)
						.toList(),
				hasPaymentConsistencyMetadata(mappings)
		);
	}

	default EvidenceQueryResult unknown(LokiEvidenceQuery query) {
		Objects.requireNonNull(query, "query must not be null");
		return new EvidenceQueryResult(
				EvidenceSourceType.LOGS,
				EvidenceCollectionStatus.UNKNOWN,
				List.of(),
				false
		);
	}

	default EvidenceQueryResult failed(LokiEvidenceQuery query) {
		Objects.requireNonNull(query, "query must not be null");
		return new EvidenceQueryResult(
				EvidenceSourceType.LOGS,
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
			List<LokiEvidenceMapping> mappings
	) {
		return mappings.stream().anyMatch(
				mapping -> mapping.semanticType()
						== LokiLogSemanticType.PAYMENT_CONSISTENCY_EVENT
						&& mapping.sanitizedConsistencyMetadataPresent()
		);
	}
}
