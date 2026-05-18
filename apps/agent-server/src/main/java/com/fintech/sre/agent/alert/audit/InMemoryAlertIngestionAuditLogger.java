package com.fintech.sre.agent.alert.audit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

@Component
public class InMemoryAlertIngestionAuditLogger implements AlertIngestionAuditLogger {

	private final List<AlertIngestionAuditLog> logs =
			Collections.synchronizedList(new ArrayList<>());

	@Override
	public Mono<Void> log(AlertIngestionAuditLog auditLog) {
		if (auditLog != null) {
			logs.add(auditLog);
		}
		return Mono.empty();
	}

	public List<AlertIngestionAuditLog> logs() {
		synchronized (logs) {
			return List.copyOf(logs);
		}
	}
}
