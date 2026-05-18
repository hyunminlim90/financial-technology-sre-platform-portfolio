package com.fintech.sre.agent.learning.candidate;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class LearningCandidateIdGenerator {

	public String generate() {
		return "learning-candidate-"
				+ Instant.now().toString().replace(":", "-")
				+ "-"
				+ UUID.randomUUID();
	}
}
