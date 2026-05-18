package com.fintech.sre.agent.alert.audit;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class AlertIngestionAuditIdGenerator {

	public String generate() {
		return "alert-ingestion-"
				+ Instant.now().toString().replace(":", "-")
				+ "-"
				+ UUID.randomUUID();
	}
}
