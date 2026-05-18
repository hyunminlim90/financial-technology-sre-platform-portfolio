package com.fintech.sre.agent.admin.knowledge.audit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

@Component
public class InMemoryKnowledgeIngestionAuditLogger implements KnowledgeIngestionAuditLogger {

	private final List<KnowledgeIngestionAuditLog> logs =
			Collections.synchronizedList(new ArrayList<>());

	@Override
	public Mono<Void> log(KnowledgeIngestionAuditLog auditLog) {
		if (auditLog != null) {
			logs.add(auditLog);
		}

		return Mono.empty();
	}

	public List<KnowledgeIngestionAuditLog> logs() {
		synchronized (logs) {
			return List.copyOf(logs);
		}
	}
}
