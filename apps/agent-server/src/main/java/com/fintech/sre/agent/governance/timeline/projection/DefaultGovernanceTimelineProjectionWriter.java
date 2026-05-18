package com.fintech.sre.agent.governance.timeline.projection;

import java.util.Objects;

import com.fintech.sre.agent.governance.timeline.GovernanceTimelineProjection;

import reactor.core.publisher.Mono;

public class DefaultGovernanceTimelineProjectionWriter
		implements GovernanceTimelineProjectionWriter {

	private final GovernanceTimelineProjectionRecordMapper mapper;
	private final GovernanceTimelineProjectionStore store;
	private final GovernanceTimelineProjectionMetricsRecorder metricsRecorder;

	public DefaultGovernanceTimelineProjectionWriter(
			GovernanceTimelineProjectionRecordMapper mapper,
			GovernanceTimelineProjectionStore store,
			GovernanceTimelineProjectionMetricsRecorder metricsRecorder
	) {
		this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
		this.store = Objects.requireNonNull(store, "store must not be null");
		this.metricsRecorder = Objects.requireNonNull(
				metricsRecorder,
				"metricsRecorder must not be null"
		);
	}

	@Override
	public Mono<GovernanceTimelineProjectionWriteResult> write(
			GovernanceTimelineProjection projection
	) {
		Objects.requireNonNull(projection, "projection must not be null");

		GovernanceTimelineProjectionRecord record = mapper.map(projection);
		return store.append(record)
				.doOnNext(result -> metricsRecorder.write(result.status()))
				.doOnError(error -> metricsRecorder.failure());
	}
}
