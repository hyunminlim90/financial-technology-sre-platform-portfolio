package com.fintech.sre.agent.embedding;

import java.util.List;

public record EmbeddingResponse(
		List<Double> vector,
		String model
) {
}
