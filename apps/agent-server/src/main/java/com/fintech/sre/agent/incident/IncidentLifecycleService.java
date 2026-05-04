package com.fintech.sre.agent.incident;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class IncidentLifecycleService {

	private final IncidentLifecycleRepository repository;

	public IncidentLifecycleService(IncidentLifecycleRepository repository) {
		this.repository = repository;
	}

	public Mono<IncidentLifecycle> createIfAbsent(String incidentId) {
		return repository.findByIncidentId(incidentId)
				.switchIfEmpty(Mono.defer(() -> {
					Instant now = Instant.now();

					IncidentLifecycle lifecycle = new IncidentLifecycle(
							incidentId,
							IncidentStatus.DETECTED,
							List.of("%s -> %s : incident detected".formatted(now, IncidentStatus.DETECTED)),
							now,
							now
					);

					return repository.save(lifecycle);
				}));
	}

	public Mono<IncidentLifecycle> transition(
			String incidentId,
			IncidentStatus nextStatus,
			String reason
	) {
		return createIfAbsent(incidentId)
				.flatMap(current -> {
					if (!isAllowed(current.status(), nextStatus)) {
						return Mono.error(new IllegalStateException(
								"Invalid incident transition: %s -> %s".formatted(
										current.status(),
										nextStatus
								)
						));
					}

					return repository.save(current.transitionTo(nextStatus, reason));
				});
	}

	public Mono<IncidentLifecycle> advanceTo(
			String incidentId,
			IncidentStatus targetStatus,
			String reason
	) {
		return createIfAbsent(incidentId)
				.flatMap(current -> {
					if (current.status() == targetStatus) {
						return Mono.just(current);
					}

					List<IncidentStatus> path = path(current.status(), targetStatus);
					if (path.isEmpty()) {
						return Mono.error(new IllegalStateException(
								"Invalid incident transition: %s -> %s".formatted(
										current.status(),
										targetStatus
								)
						));
					}

					Mono<IncidentLifecycle> chain = Mono.just(current);
					for (IncidentStatus next : path) {
						chain = chain.flatMap(lifecycle -> repository.save(lifecycle.transitionTo(next, reason)));
					}
					return chain;
				});
	}

	public Mono<IncidentLifecycle> findByIncidentId(String incidentId) {
		return repository.findByIncidentId(incidentId)
				.switchIfEmpty(Mono.error(new IllegalArgumentException(
						"IncidentLifecycle not found: " + incidentId
				)));
	}

	public Flux<IncidentLifecycle> findAll() {
		return repository.findAll();
	}

	private List<IncidentStatus> path(IncidentStatus current, IncidentStatus target) {
		List<IncidentStatus> ordered = List.of(
				IncidentStatus.DETECTED,
				IncidentStatus.RECOMMENDATION_CREATED,
				IncidentStatus.HUMAN_REVIEW_REQUIRED,
				IncidentStatus.ACTION_APPROVED,
				IncidentStatus.OUTCOME_REPORTED,
				IncidentStatus.POSTMORTEM_DRAFT_READY,
				IncidentStatus.IMPROVEMENT_CANDIDATES_CREATED,
				IncidentStatus.KNOWLEDGE_REVIEW_REQUESTED,
				IncidentStatus.CLOSED
		);

		if (current == IncidentStatus.ACTION_REJECTED) {
			return target == IncidentStatus.CLOSED ? List.of(IncidentStatus.CLOSED) : List.of();
		}

		if (target == IncidentStatus.ACTION_REJECTED) {
			return current == IncidentStatus.HUMAN_REVIEW_REQUIRED ? List.of(IncidentStatus.ACTION_REJECTED) : List.of();
		}

		int currentIndex = ordered.indexOf(current);
		int targetIndex = ordered.indexOf(target);
		if (currentIndex < 0 || targetIndex < 0 || currentIndex > targetIndex) {
			return List.of();
		}

		return ordered.subList(currentIndex + 1, targetIndex + 1);
	}

	private boolean isAllowed(IncidentStatus current, IncidentStatus next) {
		if (current == next) {
			return true;
		}

		return switch (current) {
			case DETECTED -> next == IncidentStatus.RECOMMENDATION_CREATED;
			case RECOMMENDATION_CREATED -> next == IncidentStatus.HUMAN_REVIEW_REQUIRED;
			case HUMAN_REVIEW_REQUIRED -> next == IncidentStatus.ACTION_APPROVED
					|| next == IncidentStatus.ACTION_REJECTED;
			case ACTION_APPROVED -> next == IncidentStatus.OUTCOME_REPORTED;
			case ACTION_REJECTED -> next == IncidentStatus.CLOSED;
			case OUTCOME_REPORTED -> next == IncidentStatus.POSTMORTEM_DRAFT_READY;
			case POSTMORTEM_DRAFT_READY -> next == IncidentStatus.IMPROVEMENT_CANDIDATES_CREATED;
			case IMPROVEMENT_CANDIDATES_CREATED -> next == IncidentStatus.KNOWLEDGE_REVIEW_REQUESTED
					|| next == IncidentStatus.CLOSED;
			case KNOWLEDGE_REVIEW_REQUESTED -> next == IncidentStatus.CLOSED;
			case CLOSED -> false;
		};
	}
}
