package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record ScenarioReference(
		String scenarioId,
		String knowledgeSourceId,
		boolean known,
		boolean deprecated
) {
	public ScenarioReference {
		Objects.requireNonNull(scenarioId, "scenarioId must not be null");
		Objects.requireNonNull(
				knowledgeSourceId,
				"knowledgeSourceId must not be null"
		);
	}
}
