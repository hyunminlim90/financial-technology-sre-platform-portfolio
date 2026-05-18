package com.fintech.sre.agent.knowledge.vector.upsert;

import java.util.List;

public record VectorUpsertResult(
		List<String> upsertedPointIds,
		List<VectorUpsertFailure> failures
) {
	public VectorUpsertResult {
		upsertedPointIds = upsertedPointIds == null ? List.of() : List.copyOf(upsertedPointIds);
		failures = failures == null ? List.of() : List.copyOf(failures);
	}

	public static VectorUpsertResult empty() {
		return new VectorUpsertResult(List.of(), List.of());
	}

	public static VectorUpsertResult success(List<String> pointIds) {
		return new VectorUpsertResult(
				pointIds == null ? List.of() : List.copyOf(pointIds),
				List.of()
		);
	}

	public static VectorUpsertResult failed(List<VectorUpsertFailure> failures) {
		return new VectorUpsertResult(
				List.of(),
				failures == null ? List.of() : List.copyOf(failures)
		);
	}
}
