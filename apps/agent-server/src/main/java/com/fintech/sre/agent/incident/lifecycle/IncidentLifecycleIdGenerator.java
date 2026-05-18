package com.fintech.sre.agent.incident.lifecycle;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class IncidentLifecycleIdGenerator {

	public String generate() {
		return "incident-lifecycle-"
				+ Instant.now().toString().replace(":", "-")
				+ "-"
				+ UUID.randomUUID();
	}
}
