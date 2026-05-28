package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record RollbackReference(
		String rollbackId,
		String knowledgeSourceId,
		boolean known,
		boolean deprecated
) {
	public RollbackReference {
		Objects.requireNonNull(rollbackId, "rollbackId must not be null");
		Objects.requireNonNull(
				knowledgeSourceId,
				"knowledgeSourceId must not be null"
		);
	}
}
