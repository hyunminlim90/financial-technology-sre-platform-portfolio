package com.fintech.sre.agent.alert.dedup;

import reactor.core.publisher.Mono;

public interface AlertDeduplicationStore {

	Mono<AlertDeduplicationResult> checkAndRecord(AlertDeduplicationKey key);
}
