package com.fintech.sre.agent.recommendation.execution.result;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class HumanExecutionResultIdGenerator {

	public String generate() {
		return "human-execution-result-"
				+ Instant.now().toString().replace(":", "-")
				+ "-"
				+ UUID.randomUUID();
	}
}
