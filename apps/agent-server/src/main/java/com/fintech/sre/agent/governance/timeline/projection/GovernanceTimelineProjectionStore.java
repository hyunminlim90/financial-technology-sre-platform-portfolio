package com.fintech.sre.agent.governance.timeline.projection;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface GovernanceTimelineProjectionStore {

	Mono<GovernanceTimelineProjectionWriteResult> append(
			GovernanceTimelineProjectionRecord record
	);

	Flux<GovernanceTimelineProjectionRecord> findRecent(
			int limit
	);
}
