package com.fintech.sre.agent.alert.ratelimit;

import reactor.core.publisher.Mono;

public interface AlertRateLimitStore {

	Mono<AlertRateLimitResult> checkAndConsume(AlertRateLimitKey key);
}
