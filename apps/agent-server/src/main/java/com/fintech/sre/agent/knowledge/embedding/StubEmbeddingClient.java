package com.fintech.sre.agent.knowledge.embedding;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.CRC32;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

@Component
@Profile("stub-embedding")
public class StubEmbeddingClient implements EmbeddingClient {

	private static final int VECTOR_SIZE = 8;

	@Override
	public Mono<EmbeddingResult> embed(List<EmbeddingRequest> requests) {
		if (requests == null || requests.isEmpty()) {
			return Mono.just(EmbeddingResult.empty());
		}

		List<EmbeddingVector> vectors = requests.stream()
				.map(this::toVector)
				.toList();

		return Mono.just(EmbeddingResult.success(vectors));
	}

	private EmbeddingVector toVector(EmbeddingRequest request) {
		return new EmbeddingVector(
				request.chunkId(),
				request.documentId(),
				deterministicVector(request.input()),
				request.payload()
		);
	}

	private List<Float> deterministicVector(String input) {
		String safeInput = input == null ? "" : input;

		CRC32 crc32 = new CRC32();
		crc32.update(safeInput.getBytes(StandardCharsets.UTF_8));

		long seed = crc32.getValue();

		return java.util.stream.IntStream.range(0, VECTOR_SIZE)
				.mapToObj(index -> normalize(seed, index))
				.toList();
	}

	private float normalize(long seed, int index) {
		long value = (seed >> (index % 16)) & 0xFF;
		return value / 255.0f;
	}
}
