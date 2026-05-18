package com.fintech.sre.agent.recommendation.execution;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class ExecutionPlanIdGenerator {

	public String generate() {
		return "execution-plan-"
				+ Instant.now().toString().replace(":", "-")
				+ "-"
				+ UUID.randomUUID();
	}
}
