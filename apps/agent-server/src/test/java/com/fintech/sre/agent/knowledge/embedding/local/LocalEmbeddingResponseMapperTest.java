package com.fintech.sre.agent.knowledge.embedding.local;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.knowledge.embedding.EmbeddingRequest;
import com.fintech.sre.agent.knowledge.embedding.EmbeddingResult;

class LocalEmbeddingResponseMapperTest {

	private final LocalEmbeddingResponseMapper mapper = new LocalEmbeddingResponseMapper();

	@Test
	void shouldMapVectorsByIndex() {
		List<EmbeddingRequest> requests = List.of(
				request("chunk-1"),
				request("chunk-2")
		);

		LocalEmbeddingResponse response = new LocalEmbeddingResponse(List.of(
				new LocalEmbeddingResponse.Item(1, List.of(0.2f, 0.3f)),
				new LocalEmbeddingResponse.Item(0, List.of(0.1f, 0.2f))
		));

		EmbeddingResult result = mapper.toResult(requests, response);

		assertThat(result.vectors()).hasSize(2);
		assertThat(result.vectors().get(0).chunkId()).isEqualTo("chunk-1");
		assertThat(result.vectors().get(1).chunkId()).isEqualTo("chunk-2");
		assertThat(result.failures()).isEmpty();
	}

	@Test
	void shouldReturnFailureWhenVectorMissing() {
		List<EmbeddingRequest> requests = List.of(
				request("chunk-1"),
				request("chunk-2")
		);

		LocalEmbeddingResponse response = new LocalEmbeddingResponse(List.of(
				new LocalEmbeddingResponse.Item(0, List.of(0.1f, 0.2f))
		));

		EmbeddingResult result = mapper.toResult(requests, response);

		assertThat(result.vectors()).hasSize(1);
		assertThat(result.failures()).hasSize(1);
		assertThat(result.failures().get(0).chunkId()).isEqualTo("chunk-2");
		assertThat(result.failures().get(0).reasonCode())
				.isEqualTo("LOCAL_EMBEDDING_MISSING_VECTOR");
	}

	private EmbeddingRequest request(String chunkId) {
		return new EmbeddingRequest(
				chunkId,
				"doc-1",
				"content",
				Map.of("type", "RUNBOOK"),
				List.of("RUNBOOK")
		);
	}
}
