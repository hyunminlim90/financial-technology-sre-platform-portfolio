package com.fintech.sre.agent.knowledge.vector.upsert;

import reactor.core.publisher.Mono;

public interface VectorStoreUpsertClient {

	Mono<VectorUpsertResult> upsert(VectorUpsertRequest request);
}
