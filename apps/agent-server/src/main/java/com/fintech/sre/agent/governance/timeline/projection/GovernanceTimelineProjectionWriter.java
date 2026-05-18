package com.fintech.sre.agent.governance.timeline.projection;

import com.fintech.sre.agent.governance.timeline.GovernanceTimelineProjection;

import reactor.core.publisher.Mono;

public interface GovernanceTimelineProjectionWriter {

	Mono<GovernanceTimelineProjectionWriteResult> write(
			GovernanceTimelineProjection projection
	);
}
