package com.fintech.sre.agent.alert.audit;

import reactor.core.publisher.Mono;

public interface AlertIngestionAuditLogger {

	Mono<Void> log(AlertIngestionAuditLog auditLog);
}
