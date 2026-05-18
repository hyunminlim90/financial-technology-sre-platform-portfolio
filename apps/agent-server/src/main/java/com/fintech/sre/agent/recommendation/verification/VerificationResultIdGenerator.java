package com.fintech.sre.agent.recommendation.verification;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class VerificationResultIdGenerator {

	public String generate() {
		return "verification-result-"
				+ Instant.now().toString().replace(":", "-")
				+ "-"
				+ UUID.randomUUID();
	}
}
