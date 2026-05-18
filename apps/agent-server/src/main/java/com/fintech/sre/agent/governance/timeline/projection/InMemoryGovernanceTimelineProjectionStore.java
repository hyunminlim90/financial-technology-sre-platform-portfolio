package com.fintech.sre.agent.governance.timeline.projection;

import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class InMemoryGovernanceTimelineProjectionStore
		implements GovernanceTimelineProjectionStore {

	private final ConcurrentMap<String, GovernanceTimelineProjectionRecord> records =
			new ConcurrentHashMap<>();

	@Override
	public Mono<GovernanceTimelineProjectionWriteResult> append(
			GovernanceTimelineProjectionRecord record
	) {
		Objects.requireNonNull(record, "record must not be null");

		GovernanceTimelineProjectionRecord existing =
				records.putIfAbsent(record.eventId(), record);

		if (existing != null) {
			return Mono.just(
					GovernanceTimelineProjectionWriteResult.duplicateSkipped(
							record.eventId()
					)
			);
		}

		return Mono.just(
				GovernanceTimelineProjectionWriteResult.appended(record.eventId())
		);
	}

	@Override
	public Flux<GovernanceTimelineProjectionRecord> findRecent(int limit) {
		if (limit <= 0) {
			return Flux.empty();
		}

		return Flux.fromIterable(records.values())
				.sort(
						Comparator
								.comparing(GovernanceTimelineProjectionRecord::occurredAt)
								.reversed()
								.thenComparing(
										GovernanceTimelineProjectionRecord::eventId,
										Comparator.reverseOrder()
								)
				)
				.take(limit);
	}
}
