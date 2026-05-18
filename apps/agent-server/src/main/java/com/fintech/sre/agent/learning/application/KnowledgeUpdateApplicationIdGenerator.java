package com.fintech.sre.agent.learning.application;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class KnowledgeUpdateApplicationIdGenerator {

	public String generate() {
		return "knowledge-update-"
				+ Instant.now().toString().replace(":", "-")
				+ "-"
				+ UUID.randomUUID();
	}
}
