package com.fintech.sre.agent.knowledge.qdrant;

import java.util.List;
import java.util.Map;

public record QdrantScrollResponse(
		Result result,
		String status,
		double time
) {
	public record Result(
			List<Point> points
	) {
	}

	public record Point(
			String id,
			Map<String, Object> payload
	) {
	}
}
