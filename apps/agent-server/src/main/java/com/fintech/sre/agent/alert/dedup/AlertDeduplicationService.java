package com.fintech.sre.agent.alert.dedup;

import org.springframework.stereotype.Service;

import com.fintech.sre.agent.alert.AlertEvent;

import reactor.core.publisher.Mono;

@Service
public class AlertDeduplicationService {

	private final AlertDeduplicationStore store;

	public AlertDeduplicationService(AlertDeduplicationStore store) {
		this.store = store;
	}

	public Mono<AlertDeduplicationResult> checkAndRecord(AlertEvent alert) {
		AlertDeduplicationKey key = AlertDeduplicationKey.of(
				alert == null ? null : alert.alertName(),
				alert == null ? null : alert.service(),
				alert == null ? null : alert.status()
		);

		return store.checkAndRecord(key);
	}
}
