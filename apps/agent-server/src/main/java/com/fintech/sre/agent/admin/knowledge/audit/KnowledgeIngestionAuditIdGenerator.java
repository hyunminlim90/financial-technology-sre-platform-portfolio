package com.fintech.sre.agent.admin.knowledge.audit;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class KnowledgeIngestionAuditIdGenerator {

	public String generate() {
		return "knowledge-ingestion-"
				+ Instant.now().toString().replace(":", "-")
				+ "-"
				+ UUID.randomUUID();
	}
}
