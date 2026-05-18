package com.fintech.sre.agent.knowledge.embedding.local;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.knowledge.embedding.EmbeddingFailure;
import com.fintech.sre.agent.knowledge.embedding.EmbeddingRequest;
import com.fintech.sre.agent.knowledge.embedding.EmbeddingResult;
import com.fintech.sre.agent.knowledge.embedding.EmbeddingVector;

@Component
public class LocalEmbeddingResponseMapper {

	public EmbeddingResult toResult(
			List<EmbeddingRequest> requests,
			LocalEmbeddingResponse response
	) {
		if (response == null || response.data() == null || response.data().isEmpty()) {
			return failed(
					requests,
					"LOCAL_EMBEDDING_EMPTY_RESPONSE",
					"Local embedding provider returned empty response."
			);
		}

		Map<Integer, List<Float>> vectorsByIndex = response.data().stream()
				.filter(item -> item.embedding() != null && !item.embedding().isEmpty())
				.collect(java.util.stream.Collectors.toMap(
						LocalEmbeddingResponse.Item::index,
						LocalEmbeddingResponse.Item::embedding,
						(left, right) -> left
				));

		List<EmbeddingVector> vectors = java.util.stream.IntStream.range(0, requests.size())
				.filter(vectorsByIndex::containsKey)
				.mapToObj(index -> new EmbeddingVector(
						requests.get(index).chunkId(),
						requests.get(index).documentId(),
						vectorsByIndex.get(index),
						requests.get(index).payload()
				))
				.toList();

		List<EmbeddingFailure> failures = java.util.stream.IntStream.range(0, requests.size())
				.filter(index -> !vectorsByIndex.containsKey(index))
				.mapToObj(index -> new EmbeddingFailure(
						requests.get(index).chunkId(),
						"LOCAL_EMBEDDING_MISSING_VECTOR",
						"Local embedding provider did not return a vector for this chunk."
				))
				.toList();

		return new EmbeddingResult(vectors, failures);
	}

	public EmbeddingResult failed(
			List<EmbeddingRequest> requests,
			String reasonCode,
			String reason
	) {
		if (requests == null || requests.isEmpty()) {
			return EmbeddingResult.empty();
		}

		return EmbeddingResult.failed(
				requests.stream()
						.map(request -> new EmbeddingFailure(
								request.chunkId(),
								reasonCode,
								reason
						))
						.toList()
		);
	}
}
