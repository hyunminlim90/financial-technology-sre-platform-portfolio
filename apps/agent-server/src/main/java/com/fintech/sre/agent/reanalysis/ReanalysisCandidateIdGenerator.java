package com.fintech.sre.agent.reanalysis;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class ReanalysisCandidateIdGenerator {

	public String generate() {
		return "reanalysis-candidate-"
				+ Instant.now().toString().replace(":", "-")
				+ "-"
				+ UUID.randomUUID();
	}
}
