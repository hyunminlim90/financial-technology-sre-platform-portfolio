package com.fintech.sre.agent.admin.knowledge.audit;

import reactor.core.publisher.Mono;

public interface KnowledgeIngestionAuditLogger {

	Mono<Void> log(KnowledgeIngestionAuditLog auditLog);
}
