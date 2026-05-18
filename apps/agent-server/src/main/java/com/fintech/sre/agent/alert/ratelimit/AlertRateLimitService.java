package com.fintech.sre.agent.alert.ratelimit;

import org.springframework.stereotype.Service;

import com.fintech.sre.agent.alert.AlertEvent;

import reactor.core.publisher.Mono;

@Service
public class AlertRateLimitService {

	private final AlertRateLimitStore store;

	public AlertRateLimitService(AlertRateLimitStore store) {
		this.store = store;
	}

	public Mono<AlertRateLimitResult> checkAndConsume(AlertEvent alert) {
		AlertRateLimitKey key = AlertRateLimitKey.of(
				alert == null ? null : alert.service(),
				alert == null || alert.severity() == null ? null : alert.severity().name()
		);

		return store.checkAndConsume(key);
	}
}
