package com.fintech.sre.agent.knowledge.vector.upsert;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

@Component
@Profile("!qdrant")
public class NoopVectorStoreUpsertClient implements VectorStoreUpsertClient {

	@Override
	public Mono<VectorUpsertResult> upsert(VectorUpsertRequest request) {
		if (request == null || request.isEmpty()) {
			return Mono.just(VectorUpsertResult.empty());
		}

		return Mono.just(VectorUpsertResult.failed(
				request.vectors().stream()
						.map(vector -> new VectorUpsertFailure(
								vector.chunkId(),
								"VECTOR_STORE_NOT_CONFIGURED",
								"No vector store upsert client configured. Vector was not persisted."
						))
						.toList()
		));
	}
}
