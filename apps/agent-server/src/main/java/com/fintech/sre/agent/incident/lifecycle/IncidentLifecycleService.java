package com.fintech.sre.agent.incident.lifecycle;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fintech.sre.agent.observability.metrics.IncidentLifecycleMetricsRecorder;
import reactor.core.publisher.Mono;

@Service("internalIncidentLifecycleService")
public class IncidentLifecycleService {

	private final IncidentLifecycleStore store;
	private final IncidentLifecycleIdGenerator idGenerator;
	private final IncidentLifecycleTransitionValidator validator;
	private final IncidentLifecycleMetricsRecorder metricsRecorder;

	public IncidentLifecycleService(
			IncidentLifecycleStore store,
			IncidentLifecycleIdGenerator idGenerator,
			IncidentLifecycleTransitionValidator validator,
			IncidentLifecycleMetricsRecorder metricsRecorder
	) {
		this.store = store;
		this.idGenerator = idGenerator;
		this.validator = validator;
		this.metricsRecorder = metricsRecorder;
	}

	public Mono<IncidentLifecycleTransitionResponse> transition(
			String incidentId,
			IncidentLifecycleTransitionRequest request
	) {
		return validate(request)
				.then(store.findLatestByIncidentId(incidentId)
						.defaultIfEmpty(initialRecord(incidentId)))
				.flatMap(previous -> saveTransition(
						incidentId,
						previous.currentStatus(),
						request
				));
	}

	public Mono<IncidentLifecycleRecord> latest(
			String incidentId
	) {
		return store.findLatestByIncidentId(incidentId);
	}

	private Mono<Void> validate(
			IncidentLifecycleTransitionRequest request
	) {
		if (request == null) {
			return Mono.error(
					new IncidentLifecycleRejectedException(
							"INCIDENT_TRANSITION_REQUEST_REQUIRED",
							"Transition request is required."
					)
			);
		}

		if (request.operatorId() == null
				|| request.operatorId().isBlank()) {
			return Mono.error(
					new IncidentLifecycleRejectedException(
							"OPERATOR_ID_REQUIRED",
							"operatorId is required."
					)
			);
		}

		if (request.summary() == null
				|| request.summary().isBlank()) {
			return Mono.error(
					new IncidentLifecycleRejectedException(
							"INCIDENT_TRANSITION_SUMMARY_REQUIRED",
							"summary is required."
					)
			);
		}

		if (request.transitionReason() == null) {
			return Mono.error(
					new IncidentLifecycleRejectedException(
							"INCIDENT_TRANSITION_REASON_REQUIRED",
							"transitionReason is required."
					)
			);
		}

		return Mono.empty();
	}

	private Mono<IncidentLifecycleTransitionResponse> saveTransition(
			String incidentId,
			IncidentStatus previousStatus,
			IncidentLifecycleTransitionRequest request
	) {
		validator.validate(previousStatus, request.toStatus());

		IncidentLifecycleRecord record =
				new IncidentLifecycleRecord(
						idGenerator.generate(),
						incidentId,
						previousStatus,
						request.toStatus(),
						request.transitionReason(),
						request.operatorId(),
						request.summary(),
						Instant.now(),
						sanitizeMetadata(request.metadata())
				);

		return store.save(record)
				.doOnNext(metricsRecorder::recordTransition)
				.map(this::toResponse);
	}

	private IncidentLifecycleTransitionResponse toResponse(
			IncidentLifecycleRecord record
	) {
		return new IncidentLifecycleTransitionResponse(
				record.incidentLifecycleId(),
				record.incidentId(),
				record.previousStatus(),
				record.currentStatus(),
				record.transitionReason(),
				record.operatorId(),
				record.summary()
		);
	}

	private IncidentLifecycleRecord initialRecord(
			String incidentId
	) {
		return new IncidentLifecycleRecord(
				"INITIAL",
				incidentId,
				null,
				null,
				null,
				"SYSTEM",
				"Initial lifecycle state.",
				Instant.EPOCH,
				Map.of()
		);
	}

	private Map<String, String> sanitizeMetadata(
			Map<String, String> metadata
	) {
		if (metadata == null || metadata.isEmpty()) {
			return Map.of();
		}

		return metadata.entrySet().stream()
				.filter(entry -> allowed(entry.getKey()))
				.collect(Collectors.toUnmodifiableMap(
						Map.Entry::getKey,
						Map.Entry::getValue
				));
	}

	private boolean allowed(String key) {
		if (key == null) {
			return false;
		}

		String lower = key.toLowerCase();

		return !lower.contains("payload")
				&& !lower.contains("customer")
				&& !lower.contains("secret")
				&& !lower.contains("token")
				&& !lower.contains("payment")
				&& !lower.contains("rawlog");
	}
}
