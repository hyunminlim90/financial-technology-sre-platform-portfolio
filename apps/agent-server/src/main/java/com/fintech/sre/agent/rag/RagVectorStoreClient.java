package com.fintech.sre.agent.rag;

import java.util.List;

import reactor.core.publisher.Mono;

public interface RagVectorStoreClient {

	Mono<List<RagDocument>> search(RagSearchQuery query);
}
